import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.functions.{col, concat_ws, udf}
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}


object Main {
  private val bootstrapServer = "localhost:29092"
  private val topicInput = "input"
  private val topicOutput = "predictition"
  val modelPath = "./src/main/resources/model/"

  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession.builder()
      .appName("MLSS")
      .config("spark.master", "local[2]")
      .config("spark.eventLog.enabled", "true")
      .config("spark.eventLog.dir", "file:///home/vadim/MyExp/spark-logs/event")
      //.config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.11:2.3.1")
      .getOrCreate()
    import spark.implicits._
    spark.sparkContext.setLogLevel("ERROR")

    val model = PipelineModel.load(modelPath)
    //A feature transformer that merges multiple columns into a vector column.
    val assembler = new VectorAssembler()
      .setInputCols(Array("sepal_length", "sepal_width", "petal_length", "petal_width"))
      .setOutputCol("features")

    val irisDf: DataFrame = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
      .option("subscribe", "input")
      .load()

    //irisDf.printSchema()
    val valueDf = irisDf.selectExpr("CAST(value AS STRING)")
    valueDf.createOrReplaceTempView("features")
    val features: DataFrame = spark.sql("select " +
      "CAST(SPLIT(value,',')[0] as Double) as sepal_length, " +
      "CAST(SPLIT(value,',')[1] as Double) as sepal_width, " +
      "CAST(SPLIT(value,',')[2] as Double) as petal_length, " +
      "CAST(SPLIT(value,',')[3] as Double) as petal_width " +
      "from features")
    val irisData = assembler.transform(features)
    val prediction = model.transform(irisData)

    val irisLabels = Map(0.0 -> "setosa", 1.0 -> "versicolor", 2.0 -> "virginica")
    val getPredictionLabel = (col: Double) => {
      irisLabels(col)
    }
    val predictionLabel = udf(getPredictionLabel)

    prediction
      .withColumn("predictionLabel", predictionLabel(col("prediction")))
      .select(
        $"predictionLabel".as("key"),
        concat_ws(",", $"sepal_length", $"sepal_width", $"petal_length", $"petal_width", $"predictionLabel").as("value")
      )
      .writeStream
      .outputMode("append")
      //.format("console")
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServer)
      .option("checkpointLocation", "./src/main/resources/checkpoint")
      .option("topic", "prediction")
      .start()
      .awaitTermination()

    /* Console
    features.writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
     */

  }
}