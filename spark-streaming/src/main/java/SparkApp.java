import kafka.MatchProducer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import stream.StreamJobBuilder;
import stream.StreamWinRate;

public class SparkApp {

    public static void main(String[] args) {
        try {
            SparkConf conf = new SparkConf().setAppName("LOLAnal").setMaster("local[4]");
            conf.set("spark.streaming.concurrentJobs", "2");
            conf.set("spark.scheduler.mode", "FAIR");
            conf.set("spark.streaming.stopGracefullyOnShutdown", "true");

            JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));
            jssc.sparkContext().setLogLevel("ERROR");
            jssc.checkpoint("/Users/Trayvon/Desktop/streaming_checkpoint");

            Thread matchProducer = new Thread(new MatchProducer());
            matchProducer.start();
            System.out.println("----------Match producer starts----------");

            StreamJobBuilder job = new StreamWinRate(jssc);
            job.buildJob();
            jssc.start();
            jssc.awaitTermination();
            jssc.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
