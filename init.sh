./kafka-topics.sh -create -topic input -bootstrap-server localhost:29092

./kafka-topics.sh -create -topic predictition -bootstrap-server localhost:29092

./kafka-console-producer.sh --topic input --bootstrap-server localhost:29092

./kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic input
./kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic prediction

/home/vadim/MyExp/IdeaProjects/SparkDeveloperHomeWork7_2_12/src/main/resources/send/output.sh | ./kafka-console-producer.sh --bootstrap-server localhost:29092 --topic input

./kafka-topics.sh --bootstrap-server localhost:29092 --delete --topic input
./kafka-topics.sh --bootstrap-server localhost:29092 --delete --topic prediction