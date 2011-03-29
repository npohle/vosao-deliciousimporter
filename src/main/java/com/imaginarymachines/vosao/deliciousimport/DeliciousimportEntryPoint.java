package com.imaginarymachines.vosao.deliciousimport;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vosao.business.plugin.AbstractPluginEntryPoint;
import org.vosao.entity.PluginEntity;
import org.vosao.entity.helper.PluginHelper;
import org.vosao.entity.helper.PluginParameter;

public class DeliciousimportEntryPoint extends AbstractPluginEntryPoint {

        private static final Log logger = LogFactory.getLog(DeliciousimportEntryPoint.class);

        @Override
        public String getBundleName() {return "com.imaginarymachines.vosao.deliciousimport.messages";}
        
        @Override
        public void init() {
        	getJobs().add(new DeliciousimportJob(getConfig()));
        	}
        
        private DeliciousimportConfig getConfig() {
                PluginEntity plugin = getDao().getPluginDao().getByName("deliciousimport");
                Map<String, PluginParameter> params = PluginHelper.parseParameters(plugin);
                DeliciousimportConfig result = new DeliciousimportConfig();
                try {
                        if (params.containsKey("cron")) result.setCron(params.get("cron").getValue());
                        else result.setCron(params.get("cron").getDefaultValue());
                        
                        if (params.containsKey("deliciousrssfeed")) result.setDeliciousrssfeed(params.get("deliciousrssfeed").getValue());
                        else result.setDeliciousrssfeed(params.get("deliciousrssfeed").getDefaultValue());
                        
                        if (params.containsKey("bitlyapikey")) result.setBitlyapikey(params.get("bitlyapikey").getValue());
                        else result.setBitlyapikey(params.get("bitlyapikey").getDefaultValue());
                        
                        if (params.containsKey("recency")) result.setRecency(params.get("recency").getValue());
                        else result.setRecency(params.get("recency").getDefaultValue());

                }
                catch (Exception e) {
                        logger.error("parameter: " + e.getMessage());
                }
                return result;
        }

}