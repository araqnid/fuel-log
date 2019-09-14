import axios from "axios";
import Observable from "zen-observable";

export const localAxios = axios.create({
    headers: {
        "Accept": "application/json",
        "X-Requested-With": "XMLHttpRequest"
    }
});

export function get(url) {
    return new Observable(observer => {
        const cancelSource = axios.CancelToken.source();

        localAxios.get(url, { cancelToken: cancelSource.token }).then(
            (response) => {
                observer.next(response.data);
                observer.complete();
            },
            (err) => {
                observer.error(err);
            });

        return () => {
            cancelSource.cancel();
        }
    });
}

export function postRaw(url, payload) {
    return new Observable(observer => {
        const cancelSource = axios.CancelToken.source();

        localAxios.post(url, payload, { cancelToken: cancelSource.token }).then(
            (response) => {
                observer.next(response);
                observer.complete();
            },
            (err) => {
                observer.error(err);
            });

        return () => {
            cancelSource.cancel();
        }
    });
}
