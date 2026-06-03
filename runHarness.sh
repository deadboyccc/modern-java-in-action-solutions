./gradlew shadowJar
java -jar build/libs/benchmarks.jar SimpleHarnessBenchmark \
  -rf json -rff results.json
cat results.json | jq '.[] | {name: .benchmark, score: .primaryMetric.score, unit: .primaryMetric.scoreUnit}'
#java -jar build/libs/benchmarks.jar StreamParallelBenchmargk