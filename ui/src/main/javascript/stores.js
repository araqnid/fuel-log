import IdentityStore from "./stores/IdentityStore";
import PurchasesStore from "./stores/PurchasesStore";
import PreferencesStore from "./stores/PreferencesStore";

export default (redux) => ({
    identity: new IdentityStore(redux),
    purchases: new PurchasesStore(redux),
    preferences: new PreferencesStore(redux)
});
