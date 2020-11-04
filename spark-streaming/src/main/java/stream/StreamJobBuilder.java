package stream;

import org.apache.spark.streaming.api.java.JavaStreamingContext;



public abstract class StreamJobBuilder {

    protected JavaStreamingContext jssc;

    protected StreamJobBuilder(JavaStreamingContext jssc) {
        this.jssc = jssc;
    }
    protected StreamJobBuilder() {

    }

    public abstract void buildJob();
}
