package com.xxl.job.core.biz;

import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

/**
 * Created by xuxueli on 17/3/1.
 */
public interface ExecutorBiz {

    /**
     * 心跳测试
     *
     * @return
     */
    ReturnT<String> beat();

    /**
     * idle beat
     *
     * @param jobId
     * @return
     */
     ReturnT<String> idleBeat(int jobId);

    /**
     * 停止任务执行
     *
     * @param jobId
     * @return
     */
    ReturnT<String> kill(int jobId);

    /**
     * 获取日志信息
     *
     * @param logDateTim  日志时间
     * @param logId       日志id
     * @param fromLineNum 开始行数
     * @return 日志信息
     */
    ReturnT<LogResult> log(long logDateTim, long logId, int fromLineNum);

    /**
     * run
     *
     * @param triggerParam
     * @return
     */
    ReturnT<String> run(TriggerParam triggerParam);
}
