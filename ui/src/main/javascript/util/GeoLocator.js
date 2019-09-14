import Observable from "zen-observable";

export const geoLocationSource = new Observable(observer => {
    if (!navigator.geolocation) {
        observer.error(new Error("not available"));
        return;
    }

    const watchId = navigator.geolocation.watchPosition(
        position => observer.next(position),
        error => observer.error(error),
        {
            maximumAge: 60000
        }
    );
    return () => {
        navigator.geolocation.clearWatch(watchId);
    };
});
