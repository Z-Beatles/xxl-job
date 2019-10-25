package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.core.enums.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * job registry instance
 * <p>
 * 用于维护执行器在线机器地址列表
 *
 * @author xuxueli 2016-10-02 19:10:24
 */
public class JobRegistryMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobRegistryMonitorHelper.class);

    private static JobRegistryMonitorHelper instance = new JobRegistryMonitorHelper();

    public static JobRegistryMonitorHelper getInstance() {
        return instance;
    }

    private Thread registryThread;
    private volatile boolean toStop = false;

    public void start() {
        registryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop) {
                    try {
                        // 查询所有执行器地址为自动注册的执行器
                        List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAddressType(0);
                        if (groupList != null && !groupList.isEmpty()) {

                            // 移除所有超时的在线执行器 (admin/executor)
                            List<Integer> ids = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findDead(RegistryConfig.DEAD_TIMEOUT);
                            if (ids != null && ids.size() > 0) {
                                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().removeDead(ids);
                            }

                            // 刷新所有未超时的在线执行器 (admin/executor)
                            HashMap<String, List<String>> appAddressMap = new HashMap<>();
                            List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findAll(RegistryConfig.DEAD_TIMEOUT);
                            if (list != null) {
                                for (XxlJobRegistry item : list) {
                                    if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                        // registryKey为执行器名称 example: xxl-job-executor-sample
                                        String appName = item.getRegistryKey();
                                        List<String> registryList = appAddressMap.get(appName);
                                        if (registryList == null) {
                                            registryList = new ArrayList<>();
                                        }

                                        // registryValue为执行器地址和端口 169.254.84.151:10082
                                        if (!registryList.contains(item.getRegistryValue())) {
                                            registryList.add(item.getRegistryValue());
                                        }
                                        appAddressMap.put(appName, registryList);
                                    }
                                }
                            }

                            // 更新自动注册的执行器在线机器列表
                            for (XxlJobGroup group : groupList) {
                                List<String> registryList = appAddressMap.get(group.getAppName());
                                StringBuilder addressListStr = null;
                                if (registryList != null && !registryList.isEmpty()) {
                                    Collections.sort(registryList);
                                    addressListStr = new StringBuilder();
                                    for (String item : registryList) {
                                        addressListStr.append(item).append(",");
                                    }
                                    addressListStr = new StringBuilder(addressListStr.substring(0, addressListStr.length() - 1));
                                }
                                group.setAddressList(addressListStr == null ? null : addressListStr.toString());
                                XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().update(group);
                            }
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    } catch (InterruptedException e) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
            }
        });

        registryThread.setDaemon(true);
        registryThread.setName("xxl-job, admin JobRegistryMonitorHelper");
        registryThread.start();
    }

    public void toStop() {
        toStop = true;
        // interrupt and wait
        registryThread.interrupt();
        try {
            registryThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
