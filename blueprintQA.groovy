File jsonFile = new File("${WORKSPACE}" + File.separator + "${QA_TESTS}")
Test[] tests = new groovy.json.JsonSlurper().parse(jsonFile)
String qaSeedsWorkspace = "${WORKSPACE}";

tests.each{ test ->

ArrayList testBoms = new ArrayList<String>(Arrays.asList(test.tests));
ArrayList catalogBoms = new ArrayList<String>(Arrays.asList(test.sources));

if (test.triggers.contains("pr")) {

  matrixJob("blueprintQA_pr_${test.author}_${test.name}(${test.branch})"){
      displayName("blueprintQA_pr_${test.author}/${test.name} (${test.branch})")
      runSequentially(true)
      parameters {
          stringParam('BK_URI', "${BK_URI}", 'Brooklyn URI')
          stringParam('BK_SCHEDULER_APP_ID', "${BK_SCHEDULER_APP_ID}", 'Application ID of the scheduler')
      }
      axes {
        text('BROOKLYN_TAG', test.brooklynTags)
        text('LOCATION_TAG', test.locationTags)
      }

      scm {
          git{
            remote {
              name('origin')
              github(test.repoName)
              refspec('+refs/pull/*:refs/remotes/origin/pr/*')
              credentials("${GIT_CREDENTIALS_ID}")
            }
            branch('${ghprbActualCommit}')
        }
      }
      triggers {
        if (test.triggers.contains("nightly")) {
          scm('H 0-6,18-23 * * *')
        }
        if (test.triggers.contains("pr")) {
          githubPullRequest{
            orgWhitelist('<<ADD HERE>>')
            cron('H/5 * * * *')
            allowMembersOfWhitelistedOrgsAsAdmin()
          }
        }
      }
      wrappers {
        customTools(['brooklyn-cli'])
        credentialsBinding {
          usernamePassword('BK_ADMIN_USER', 'BK_ADMIN_PASSWORD', "${BK_ADMIN_CREDENTIALS}")
         }
      }
      steps {
        test.additionalLocations.each{ id, additionalLocation ->
          shell('cp "'+qaSeedsWorkspace+'/locations/'+additionalLocation+'.bom" "${WORKSPACE}/'+additionalLocation+'.bom"')
          catalogBoms.add(additionalLocation+'.bom')
          }
        shell('cp "'+qaSeedsWorkspace+'/mergeBom.gvy" "${WORKSPACE}/mergeBom.gvy"')
        groovyScriptFile('${WORKSPACE}/mergeBom.gvy') {
           groovyInstallation('groovy-2.4.6')
           scriptParams(testBoms)
          }
        shell('mv "${WORKSPACE}/merged.output.bom" "${WORKSPACE}/merged.tests.bom"')
        groovyScriptFile('${WORKSPACE}/mergeBom.gvy') {
           groovyInstallation('groovy-2.4.6')
           scriptParams(catalogBoms)
          }
        shell('mv "${WORKSPACE}/merged.output.bom" "${WORKSPACE}/merged.catalog.bom"')
        shell('export BR_CLI_HOME="${WORKSPACE}"')
        shell('br login ${BK_URI} ${BK_ADMIN_USER} ${BK_ADMIN_PASSWORD}')
        shell('br application "${BK_SCHEDULER_APP_ID}" effector newTest invoke -P suiteName="'+test.name+'" -P ampTarget=${BROOKLYN_TAG} -P locationTarget=${LOCATION_TAG} -P app=@merged.catalog.bom -P test=@merged.tests.bom > "${WORKSPACE}/blueprint-qa-test-result.json"')
        shell('cat "${WORKSPACE}/blueprint-qa-test-result.json"')
        shell('cat "${WORKSPACE}/blueprint-qa-test-result.json" | jq .result | grep PASS')
      }
    }
  }


if (test.triggers.contains("nightly")) {
  matrixJob("blueprintQA_nightly_${test.author}_${test.name}(${test.branch})"){
      displayName("blueprintQA_nightly_${test.author}/${test.name} (${test.branch})")
      runSequentially(true)
      parameters {
          stringParam('BK_URI', "${BK_URI}", 'Brooklyn URI')
          stringParam('BK_SCHEDULER_APP_ID', "${BK_SCHEDULER_APP_ID}", 'Application ID of the scheduler')
          stringParam('QA_TEST_PATH', test.testPath, 'Path to the yaml tests')
      }
      axes {
        text('BROOKLYN_TAG', test.brooklynTags)
        text('LOCATION_TAG', test.locationTags)
      }

      scm {
          git{
            remote {
              name('origin')
              github(test.repoName)
              credentials("${GIT_CREDENTIALS_ID}")
            }
            branch(test.branch)
          }
      }
      triggers {
        scm('H 0-6,18-23 * * *')
        cron('H H * * *')
      }
      wrappers {
        customTools(['brooklyn-cli'])
        credentialsBinding {
          usernamePassword('BK_ADMIN_USER', 'BK_ADMIN_PASSWORD', "${BK_ADMIN_CREDENTIALS}")
         }
      }
      steps {
        test.additionalLocations.each{ id, additionalLocation ->
          // copy the script and the location
          String newLocation = '"${WORKSPACE}/'+additionalLocation+'.bom"'
          shell('cp "'+qaSeedsWorkspace+'/changeId.gvy" "${WORKSPACE}/changeId.gvy"')
          shell('cp "'+qaSeedsWorkspace+'/locations/'+additionalLocation+'.bom" '+newLocation)
          ArrayList params = new ArrayList<String>();
          params.add(newLocation);
          params.add(id);
          groovyScriptFile('${WORKSPACE}/changeId.gvy') {
             groovyInstallation('groovy-2.4.6')
             // change the id of the location item
             scriptParams(params)
            }
          catalogBoms.add(0,additionalLocation+'.bom')
          }
        shell('cp "'+qaSeedsWorkspace+'/mergeBom.gvy" "${WORKSPACE}/mergeBom.gvy"')
        groovyScriptFile('${WORKSPACE}/mergeBom.gvy') {
           groovyInstallation('groovy-2.4.6')
           scriptParams(testBoms)
          }
        shell('mv "${WORKSPACE}/merged.output.bom" "${WORKSPACE}/merged.tests.bom"')
        groovyScriptFile('${WORKSPACE}/mergeBom.gvy') {
           groovyInstallation('groovy-2.4.6')
           scriptParams(catalogBoms)
          }
        shell('mv "${WORKSPACE}/merged.output.bom" "${WORKSPACE}/merged.catalog.bom"')
        shell('export BR_CLI_HOME="${WORKSPACE}"')
        shell('${HOME}/brooklyn-cli/br login ${BK_URI} ${BK_ADMIN_USER} ${BK_ADMIN_PASSWORD}')
        shell('${HOME}/brooklyn-cli/br application "${BK_SCHEDULER_APP_ID}" effector newTest invoke -P suiteName="'+test.name+'" -P ampTarget=${BROOKLYN_TAG} -P locationTarget=${LOCATION_TAG} -P app=@merged.catalog.bom -P test=@merged.tests.bom > "${WORKSPACE}/blueprint-qa-test-result.json"')
        shell('cat "${WORKSPACE}/blueprint-qa-test-result.json"')
        shell('cat "${WORKSPACE}/blueprint-qa-test-result.json" | ${HOME}/jq/jq .result | grep PASS')
      }
    }
  }
}

class Test{
  String author, name, branch, repoName, testPath
  String[] locationTags, brooklynTags, sources, tests, triggers
  Map<String,String> additionalLocations
}
