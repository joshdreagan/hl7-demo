# camel-hl7-gateway

## Requirements

- [Apache Maven 3.x](http://maven.apache.org)

## Preparing

```
cd $PROJECT_ROOT
mvn clean install
```

## Running the example standalone

```
cd $PROJECT_ROOT
mvn spring-boot:run
```

## Running the example in OpenShift

```
oc new-project hl7-demo
oc create -f src/main/kube/serviceaccount.yml
oc create -f src/main/kube/configmap.yml
oc create -f src/main/kube/secret.yml
oc secrets add sa/primerica-uc2-sa secret/camel-hl7-gateway-secret
oc policy add-role-to-user view system:serviceaccount:hl7-demo:camel-hl7-gateway-sa
mvn clean install fabric8:deploy
```

## Testing the code

There are 3 points of ingress for this application.

_File_

```
cp "$PROJECT_ROOT/src/test/data/ADT_A01.txt" "$PROJECT_ROOT/target/input/ADT_A01.hl7"
```

_MLLP_

```
cd "$PROJECT_ROOT/src/test"
./scripts/send.sh "data/ADT_A01.txt" localhost 2575
```

Alternatively, you can use the  [HAPI Test Panel](https://hapifhir.github.io/hapi-hl7v2/hapi-testpanel/) to send in HL7v2/MLLP messages. You can use the sample HL7v2 file located in `src/test/data`.

_HTTP_

```
curl -X POST -H 'Content-Type: text/plain' -H 'Accept: text/plain' --data-binary "@data/ADT_A01.txt" 'http://localhost:8080/camel/hl7v2'
```