package Shop;

import Shop.Blacksmith.Blacksmith;
import Shop.Merchant.Merchant;

public class Shop {
    private Merchant merchant;
    private Blacksmith blacksmith;


    public enum PurchaseResult {
        SUCCESS, SOLD_OUT, NOT_ENOUGH_GOLD, INVENTORY_FULL, OVER_LIMIT
    }
}
