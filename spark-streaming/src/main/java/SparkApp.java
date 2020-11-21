import kafka.MatchProducer;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import stream.*;
import util.FileUtil;

public class SparkApp {

    public static void main(String[] args) {
        try {
            CommandLineParser parser = new BasicParser();
            Options options = new Options();
            options.addOption("m", "master", true, "master");
            options.addOption("j", "jobs", true, "jobs for streaming");
            options.addOption("c", "checkpoint", true, "checkpoint for spark");
            options.addOption("s", "source", true, "source data path");
            CommandLine commandLine = parser.parse(options, args);

            String master = commandLine.getOptionValue("m", "local[4]");
            // String defaultJobs = "StreamHeroMatches,StreamHeroWinRate,StreamPlayerWinGame,StreamSiteMessage";
            String defaultJobs = "StreamHeroMatches,StreamPlayerWinGame,StreamHeroWinRate,StreamSiteMessage";
            String jobStr = commandLine.getOptionValue("j", defaultJobs);
            String[] jobs = jobStr.split(",");
            if (commandLine.hasOption("c")) {
                FileUtil.CHECK_POINT_PATH = commandLine.getOptionValue("c");
            }
            FileUtil.createDir(FileUtil.CHECK_POINT_PATH);
            if (commandLine.hasOption("s")) {
                FileUtil.SOURCE_PATH = commandLine.getOptionValue("s");
            }

            SparkConf conf = new SparkConf().setAppName("LOLAnalStream").setMaster(master);
            conf.set("spark.streaming.concurrentJobs", String.valueOf(jobStr.length()));
            conf.set("spark.scheduler.mode", "FAIR");
            conf.set("spark.streaming.stopGracefullyOnShutdown", "true");

            JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(1));
            jssc.sparkContext().setLogLevel("ERROR");
            jssc.checkpoint(FileUtil.CHECK_POINT_PATH);

//            Thread matchProducer = new Thread(new MatchProducer());
//            matchProducer.start();
//            System.out.println("----------Match producer starts----------");

            // StreamJobBuilder job = new StreamTeamWinRate(jssc);
            // StreamJobBuilder job = new StreamHeroMatches(jssc);
            // StreamJobBuilder job = new StreamPlayerWinGame(jssc);
            // StreamJobBuilder job = new StreamSiteMessage(jssc);
            // job.buildJob();

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
