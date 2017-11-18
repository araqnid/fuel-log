import axios from "axios";
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

export const actions = dispatch => ({
    submit() {
        dispatch(({stores}) => {
            stores.purchases.submit();
        })
    }
});

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

    submit() {
        this.dispatch({ type: "NewFuelPurchaseEntry/_registering", payload: true });

        const { newPurchase: { attributes: newPurchase, geoLocation }, preferences: { preferences } } = this.getState();

        const distanceFactor = preferences.distance_unit === "MILES" ? 1.60934 : 1;
        const fuelVolumeFactor = preferences.fuel_volume_unit === "GALLONS" ? 4.54609 : 1;

        this._doSubmit({
            odometer: newPurchase.odometer * distanceFactor,
            cost: {
                currency: preferences.currency,
                amount: newPurchase.cost
            },
            fuel_volume: newPurchase.fuelVolume * fuelVolumeFactor,
            full_fill: newPurchase.fullFill,
            location: newPurchase.location,
            geo_location: geoLocation
        });
    }

    _doSubmit(newPurchase) {
        return axios.post("_api/fuel", newPurchase)
            .then(({status, headers}) => {
                if (status !== 201) {
                    this.dispatch({ type: "PurchasesStore/submission", payload: "status was " + status, error: true });
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
                this.kick();
            })
            .catch(response => {
                this.dispatch({ type: "PurchasesStore/submission", payload: response, error: true });
            });
    }
}
