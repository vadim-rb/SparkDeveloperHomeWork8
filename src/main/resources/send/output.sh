awk -F ',' 'NR > 1 { print $1 "," $2 "," $3 "," $4 }' < /home/vadim/MyExp/IdeaProjects/SparkDeveloperHomeWork7_2_12/src/main/resources/send/iris.csv | xargs -I % sh -c '{ echo %; }'