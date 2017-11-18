import {UserDataStore} from "../util/Stores";
import {AutoRefreshLoader} from "../util/Loaders";

const initialState = { purchaseList: null, loadFailure: null };

export const reducer = (state = initialState, action) => {
    switch (action.type) {
        case "PurchasesStore/loaded":
            if (action.error) {
                return { purchaseList: state.purchaseList, loadFailure: action.payload };
            }
            else {
                return { purchaseList: action.payload, loadFailure: null };
            }
        case "IdentityStore/localUserIdentity":
            return initialState;
        default:
            return state;
    }
};

export default class PurchasesStore extends UserDataStore {
    constructor(redux) {
        super(redux);
    }

    newLoader() {
        return new AutoRefreshLoader("_api/fuel", 30 * 1000, {
            foundData: data => {
                this.dispatch({ type: "PurchasesStore/loaded", payload: data });
            },

            loadError: ex => {
                this.dispatch({ type: "PurchasesStore/loaded", payload: ex, error: true });
            }
        });
    }

    submit(newPurchase) {
        this.post("_api/fuel", newPurchase)
            .then(({status, headers}) => {
                if (status !== 201) {
                    this.dispatch({ type: "PurchasesStore/submission", payload: "status was " + status, error: true });
                    this.dispatch({ type: "NewFuelPurchaseEntry/_registering", payload: false });
                    this.dispatch({ type: "NewFuelPurchaseEntry/purchaseSubmitted", payload: exception, error: true });
                    return;
                }
                const location = headers.location;
                const matches = location.match(/\/_api\/fuel\/(.+)$/);
                if (!matches) {
                    console.warn("invalid creation URI", location);
                    return;
                }
                const fuelPurchaseId = matches[1];
                this.dispatch({ type: "PurchasesStore/submission", payload: fuelPurchaseId });
                this.dispatch({ type: "NewFuelPurchaseEntry/purchaseSubmitted", payload: fuelPurchaseId });
                this.dispatch({ type: "NewFuelPurchaseEntry/_reset" });
                this.kick();
            })
            .catch(response => {
                this.dispatch({ type: "PurchasesStore/submission", payload: response, error: true });
                this.dispatch({ type: "NewFuelPurchaseEntry/_registering", payload: false });
                this.dispatch({ type: "NewFuelPurchaseEntry/purchaseSubmitted", payload: exception, error: true });
            });
    }
}
