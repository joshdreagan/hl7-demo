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
