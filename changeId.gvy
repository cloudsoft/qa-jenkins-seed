@Grab(group='org.yaml', module='snakeyaml', version='1.17')
import org.yaml.snakeyaml.Yaml

Yaml yaml = new Yaml()

if (args.length < 2) {
    println("Usage: groovy changeId.gvy <bomfile> <id>")
    }
else {
    // load the catalog specified in args[0]
    def obj = yaml.load(new FileInputStream(args[0]));
    Map sourceCatalog = obj.get("brooklyn.catalog");
    // set the new id
    sourceCatalog.put("id", args[1]);
    // write the file back out again
    FileWriter writer = new FileWriter(args[0]);
    String output = yaml.dump(obj, writer);
    }
System.exit(0);