
package com.smart.lock.entity;

import java.io.Serializable;


public class ProblemModel implements Serializable {

    // 问题
    public String question;
    // 回答
    public String answer;

    @Override
    public String toString() {
        return "ProblemModel{" +
                "question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
