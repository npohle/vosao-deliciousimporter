package com.imaginarymachines.vosao.deliciousimport;

	public class DeliciousimportConfig {

        private String cron;
        private String deliciousrssfeed;
        private String bitlyapikey;

		public DeliciousimportConfig() { 
        }

		public String getBitlyapikey() {
			return bitlyapikey;
		}

		public void setBitlyapikey(String bitlyapikey) {
			this.bitlyapikey = bitlyapikey;
		}
		
        public String getCron() {
                return cron;
        }

        public void setCron(String cron) {
                this.cron = cron;
        }

        public String getDeliciousrssfeed() {
			return deliciousrssfeed;
		}

		public void setDeliciousrssfeed(String deliciousrssfeed) {
			this.deliciousrssfeed = deliciousrssfeed;
		}
}
