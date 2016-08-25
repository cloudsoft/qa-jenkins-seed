listView('Blueprint Nightly QA Jobs') {
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex(/blueprintQA_nightly.*/)
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
listView('Blueprint PR QA Jobs') {
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex(/blueprintQA_pr_.*/)
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
listView('Atos Blueprint QA Jobs') {
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex(/.*Atos-.*/)
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
