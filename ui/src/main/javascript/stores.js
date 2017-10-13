import IdentityStore from "./stores/IdentityStore";
import PurchasesStore from "./stores/PurchasesStore";
import PreferencesStore from "./stores/PreferencesStore";

export const identity = new IdentityStore();

export const purchases = new PurchasesStore(identity);

export const preferences = new PreferencesStore(identity);
