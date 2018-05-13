import axios from "axios";
import Observable from "zen-observable";

export default function ajaxObservable(url, requestConfig) {
    return new Observable(observer => {
        const cancelSource = axios.CancelToken.source();

        axios.get(url, { ...requestConfig, cancelToken: cancelSource.token }).then(
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
