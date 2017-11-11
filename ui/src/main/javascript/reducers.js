import {combineReducers} from "redux";
import {reducer as identity} from "./stores/IdentityStore";
import {reducer as preferences} from "./stores/PreferencesStore";
import {reducer as purchases} from "./stores/PurchasesStore";
import {reducer as newPurchase} from "./NewFuelPurchaseEntry";

export default combineReducers({
    identity,
    preferences,
    purchases,
    newPurchase
});
