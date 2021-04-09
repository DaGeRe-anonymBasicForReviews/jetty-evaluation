# Jetty-Evaluation

This repository evaluates to which degree Peass is able to identify performance regressions in the application server jetty.

It is assumed that $PROJECTFOLDER is your local jetty folder (execute `git clone https://github.com/eclipse/jetty.project.git` to get the project) and that you have build the evaluation project (`mvn clean package`).

## Tree Reading

- Instrument your project folder running `java -cp target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peassEvaluation.ExecutionPreparer $PROJECTFOLDER`
- Generate the jmh jar by `mvn -V -B clean install -DskipTests -Dlicense.skip -Denforcer.skip -Dcheckstyle.skip -T6 -e -pl :jetty-jmh -am`
- Execute the measurement by `cp src/main/resources/getTrees.sh $PROJECTFOLDER/ && cd $PROJECTFOLDER && ./getTrees.sh`
- Analyse the traces running java -cp target/jetty-evaluation-0.1-SNAPSHOT.jar de.dagere.peassEvaluation.GetTrees -projectFolder $PROJECTFOLDER -tree $PROJECTFOLDER/tree-results/traces/

## Problem Injection

## Measurement with JMH

## Measurement with Peass
