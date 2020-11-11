import kafka.MatchProducer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import stream.*;
import util.FileUtil;

public class SparkApp {

    public static void main(String[] args) {
        try {
            String master = "local[4]"; // read from cmd?
            SparkConf conf = new SparkConf().setAppName("LOLAnal").setMaster(master);
            conf.set("spark.streaming.concurrentJobs", "2");
            conf.set("spark.scheduler.mode", "FAIR");
            conf.set("spark.streaming.stopGracefullyOnShutdown", "true");

            JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));
            jssc.sparkContext().setLogLevel("ERROR");
            jssc.checkpoint(FileUtil.CHECK_POINT_PATH);

            Thread matchProducer = new Thread(new MatchProducer());
            matchProducer.start();
            System.out.println("----------Match producer starts----------");

            // StreamJobBuilder job = new StreamTeamWinRate(jssc);
            // StreamJobBuilder job = new StreamHeroMessage(jssc);
            // StreamJobBuilder job = new StreamPlayerWinGame(jssc);
            // StreamJobBuilder job = new StreamSiteMessage(jssc);
            // job.buildJob();

            String[] jobs = "StreamPlayerWinGame".split(",");  // read from cmd?
            for (String className : jobs) {
                Class catClass = Class.forName("stream." + className);
                StreamJobBuilder jobBuilder = (StreamJobBuilder) catClass.newInstance();
                jobBuilder.setJssc(jssc);
                jobBuilder.buildJob();
            }

            jssc.start();
            jssc.awaitTermination();
            jssc.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
