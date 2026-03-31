package cn.itcraft.dbmmap.core.storage;

public class StoreConfig {
    
    private final StoreType storeType;
    
    private StoreConfig(Builder builder) {
        this.storeType = builder.storeType;
    }
    
    public StoreType getStoreType() { return storeType; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private StoreType storeType = StoreType.ROW;
        
        public Builder storeType(StoreType storeType) {
            this.storeType = storeType;
            return this;
        }
        
        public StoreConfig build() { return new StoreConfig(this); }
    }
}