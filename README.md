# daikon-diff-victim-polluter

Automatically diff victim+polluter invariants with victim invariants. For use with order-dependent flaky tests.

## Usage

To get the `.inv` files, go to any Java 8 Maven project and run:

```bash
./daikon-victim-polluter.sh <com.example.VictimTest> <com.example.PolluterTest>
```

```bash
mvn exec:java -Dexec.args="daikon-pv.inv daikon-victim.inv daikon-polluter.inv"
```

Or:

```bash
mvn package
java -cp target/classes:daikon-5.8.16.jar in.natelev.daikondiffvictimpolluter.DaikonDiffVictimPolluter daikon-pv.inv daikon-victim.inv daikon-polluter.inv
```

To just run the test with the polluter (no logs will be generated):

```bash
./daikon-victim-polluter.sh test <com.example.VictimTest> <com.example.PolluterTest>
```
