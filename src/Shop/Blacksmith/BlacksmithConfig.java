package Shop.Blacksmith;

import Item.Item.ItemRarity;
import Shop.Merchant.MerchantConfig;

// Every tunable number the blacksmith uses, in one place. PLACEHOLDER: if the rate changes, update
// Tests/BlacksmithConfigTests and add a line to the spec's changelog.
public final class BlacksmithConfig {
    // the fee to combine two copies of an item, as a fraction of what the merchant charges for that item
    private static final double REPAIR_RATE_FEE = 0.25;

    public static double getRepairRateFee() { return REPAIR_RATE_FEE; }

    private BlacksmithConfig() {}

    // fee = round(REPAIR_RATE_FEE * merchant price). itemWorld is the world the item belongs to.
    // a null rarity or a world outside 1-4 is rejected by MerchantConfig, so a bad input can never become a free repair
    public static int getRepairFee(ItemRarity rarity, int itemWorld) {
        return (int) Math.round(REPAIR_RATE_FEE * MerchantConfig.getGearPrice(rarity, itemWorld));
    }
}
