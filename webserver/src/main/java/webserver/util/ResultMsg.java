package webserver.util;

import lombok.Data;

@Data
public class ResultMsg<T> {

    public static String SUCCESS = "SUCCESS";

    public static String FAILURE = "FAILURE";

    private String code;

    private String msg;

    private T value;

    public ResultMsg() {
    }

    public ResultMsg(String code) {
        this.code = code;
        this.msg = "";
        this.value = null;
    }

    public ResultMsg(String code, String msg) {
        this.code = code;
        this.msg = msg;
        this.value = null;
    }

    public ResultMsg(String code, String msg, T value) {
        this.code = code;
        this.msg = msg;
        this.value = value;
    }
}
