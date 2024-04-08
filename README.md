Instalacija s:
```sh
mvn clean package
```
Pokretanje:
```sh
mvn clean compile exec:java -Dexec.mainClass="hr.fer.iot.App" -Dexec.args="-b=tcp://localhost:1884"

# -b je argument adresa brokera
```
