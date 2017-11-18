import IdentityStore from "./stores/IdentityStore";
import PurchasesStore from "./stores/PurchasesStore";

export const identity = new IdentityStore();

export const purchases = new PurchasesStore(identity);
