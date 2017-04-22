import IdentityStore from "app/stores/IdentityStore";
import PurchasesStore from "app/stores/PurchasesStore";
import PreferencesStore from "app/stores/PreferencesStore";

export const identity = new IdentityStore();

export const purchases = new PurchasesStore(identity);

export const preferences = new PreferencesStore(identity);
