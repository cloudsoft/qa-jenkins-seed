@Grab(group='org.yaml', module='snakeyaml', version='1.17')
import org.yaml.snakeyaml.Yaml

Yaml yaml = new Yaml()

// default base catalog YAML
def base = yaml.load("""
brooklyn.catalog:
  items: []""");
Map targetCatalog = base.get("brooklyn.catalog");

// standard deep copy implementation
def deepcopy(orig) {
     bos = new ByteArrayOutputStream()
     oos = new ObjectOutputStream(bos)
     oos.writeObject(orig); oos.flush()
     bin = new ByteArrayInputStream(bos.toByteArray())
     ois = new ObjectInputStream(bin)
     return ois.readObject()
}

// loop through the files to load in
for (mergeItem in args) {

    // open the file and parse the YAML
    def obj = yaml.load(new FileInputStream(mergeItem));
    Map sourceCatalog = obj.get("brooklyn.catalog");

    // merge the keys from the root object
    for (rootKey in sourceCatalog.keySet()) {
        // ignore publish sections
        if (!rootKey.equals("item") && !rootKey.equals("items")) {

            // merge to anything in to .item which isn't already there
            if (sourceCatalog.get("item") != null && sourceCatalog.get("item").get(rootKey) == null) {
                sourceCatalog.get("item").put(rootKey,deepcopy(sourceCatalog.get(rootKey)));
            }

            // merge to everything in to all the .items which aren't already there
            if (sourceCatalog.get("items") != null) {
                for (item in sourceCatalog.get("items")) {
                    // excludes items such as classpath://io.brooklyn.etcd.brooklyn-etcd:brooklyn-etcd/catalog.bom
                    if (!(item instanceof String) && item.get(rootKey) == null) item.put(rootKey,deepcopy(sourceCatalog.get(rootKey)));
                }
            }
        }
    }

    // merge any item or items into the output items
    if (sourceCatalog.get("item") != null) {
        LinkedHashMap item = new LinkedHashMap();
        item.put("item",sourceCatalog.get("item"));
        targetCatalog.get("items").add(item);
    }
    if (sourceCatalog.get("items") != null) {
        targetCatalog.get("items").addAll(sourceCatalog.get("items"));
    }
}

FileWriter writer = new FileWriter("merged.output.bom");
String output = yaml.dump(base, writer);
System.exit(0);