package com.imaginarymachines.vosao.deliciousimport;

	public class DeliciousimportConfig {

        private String cron;
        private String deliciousrssfeed;
        private String bitlyapikey;
        private String recency;

		public DeliciousimportConfig() { 
        }

		public String getBitlyapikey() {
			return bitlyapikey;
		}

		public String getRecency() {
			return recency;
		}

		public void setRecency(String recency) {
			this.recency = recency;
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
