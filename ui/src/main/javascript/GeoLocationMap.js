import React, {useEffect, useState} from "react";
import * as ajax from "./util/Ajax";

const GeoLocationMap = ({latitude, longitude}) => {
    const googleApiKey = useGoogleApiKey();
    if (!googleApiKey)
        return null;
    return (
        <div>Geo-location available:
            <img
                src={`https://maps.googleapis.com/maps/api/staticmap?center=${latitude},${longitude}&zoom=13&size=300x300&sensor=false&key=${googleApiKey}`}/>
        </div>
    );
};

export function useGoogleApiKey() {
    const [apiKey, setApiKey] = useState(null);
    useEffect(() => {
        const subscription = ajax.get("").subscribe(
            ({google_maps_api_key: value}) => setApiKey(value)
        );
        return () => {
            subscription.unsubscribe();
        }
    }, []);
    return apiKey;
}

export default GeoLocationMap;
