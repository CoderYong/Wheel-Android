package dev.yong.sample.modules.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dev.yong.sample.R;
import dev.yong.sample.data.BaseEntity;
import dev.yong.sample.data.Weather;
import dev.yong.sample.data.WeatherInfo;
import dev.yong.sample.service.ApiCacheService;
import dev.yong.sample.service.ApiService;
import dev.yong.wheel.base.mvp.IModel;
import dev.yong.wheel.cache.Cache;
import dev.yong.wheel.http.Http;
import dev.yong.wheel.http.HttpResponse;
import dev.yong.wheel.http.retrofit.RetrofitHelper;
import dev.yong.wheel.utils.TimeUtils;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.rx_cache2.DynamicKey;
import io.rx_cache2.EvictDynamicKey;
import io.rx_cache2.Reply;

/**
 * @author coderyong
 */
public class WeatherModel implements IModel {

    @Inject
    public WeatherModel() {
    }

    public void loadWeatherList(String city, final ModelCallBack<List<WeatherInfo>> callBack) {
        //获取网络数据
//        ApiCacheService.CacheProviders.getApiCache()
//                .getWeatherList(RetrofitHelper.getInstance()
//                        .baseUrl(ApiService.URL_BASE)
//                        .create(ApiService.class).getWeatherList(city), new DynamicKey(city), new EvictDynamicKey(true))
//                .map(Reply::getData)
//                .map(BaseEntity::getData)
//                .map(weathers -> createWeatherList(weathers.get(0)))
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Observer<List<WeatherInfo>>() {
//                    @Override
//                    public void onSubscribe(Disposable d) {
//
//                    }
//
//                    @Override
//                    public void onNext(List<WeatherInfo> weatherList) {
//                        callBack.onSuccess(weatherList);
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        callBack.onFail(e.getMessage());
//                    }
//
//                    @Override
//                    public void onComplete() {
//
//                    }
//                });
        final String url = "https://free-api.heweather.com/s6/weather";
        //获取缓存数据
        List<WeatherInfo> weatherList = Cache.getInstance().getList(url, WeatherInfo.class);
        if (weatherList != null) {
            callBack.onSuccess(weatherList);
            return;
        }
        //获取网络数据
        Http.request(url)
                .isCarryBasic(false)
                .addParameter("location", "auto_ip")
                .addParameter("key", "69bcb5a6d326411baccdad8ebaa06cba")
                .get(new HttpResponse<BaseEntity<List<Weather>>>() {

                    @Override
                    public void onSuccess(BaseEntity<List<Weather>> entity) {
                        if (entity == null) {
                            callBack.onSuccess(null);
                        } else {
                            List<Weather> weathers = entity.getData();
                            if (weathers != null) {
                                List<WeatherInfo> weatherList = createWeatherList(weathers.get(0));
                                Cache.getInstance().put(url, weatherList, 30, TimeUnit.SECONDS);
                                callBack.onSuccess(weatherList);
                            } else {
                                callBack.onFail(entity.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFail(Throwable t) {
                        callBack.onFail(t.getMessage());
                    }
                });
    }

    private List<WeatherInfo> createWeatherList(Weather weather) {
        List<WeatherInfo> weatherList = new ArrayList<>();
        WeatherInfo currWeather = new WeatherInfo();
        currWeather.setLocation(weather.getBasic().getLocation());
        currWeather.setItemType(WeatherInfo.TYPE_CURR_WEATHER);
        currWeather.setCurrTmp(String.format("%s℃", weather.getNow().getTmp()));
        currWeather.setMaxTmp(String.format("↑ %s°", weather.getDailyForecast().get(0).getTmpMax()));
        currWeather.setMinTmp(String.format("↓ %s°", weather.getDailyForecast().get(0).getTmpMin()));
        currWeather.setWeatherIconRes(getWeatherIconResFromText(weather.getNow().getCondTxt()));
        weatherList.add(currWeather);

        for (Weather.LifestyleEntity entity : weather.getLifestyle()) {
            WeatherInfo suggestion = new WeatherInfo();
            suggestion.setItemType(WeatherInfo.TYPE_WEATHER_SUGGEST);
            switch (entity.getType()) {
                case "drsg":
                    suggestion.setTitle(String.format("穿衣指数---%s", entity.getBrf()));
                    suggestion.setDescribe(entity.getTxt());
                    suggestion.setWeatherIconRes(R.mipmap.icon_cloth);
                    weatherList.add(suggestion);
                    break;
                case "sport":
                    suggestion.setTitle(String.format("运动指数---%s", entity.getBrf()));
                    suggestion.setDescribe(entity.getTxt());
                    suggestion.setWeatherIconRes(R.mipmap.icon_sport);
                    weatherList.add(suggestion);
                    break;
                case "trav":
                    suggestion.setTitle(String.format("旅游指数---%s", entity.getBrf()));
                    suggestion.setDescribe(entity.getTxt());
                    suggestion.setWeatherIconRes(R.mipmap.icon_travel);
                    weatherList.add(suggestion);
                    break;
                case "flu":
                    suggestion.setTitle(String.format("感冒指数---%s", entity.getBrf()));
                    suggestion.setDescribe(entity.getTxt());
                    suggestion.setWeatherIconRes(R.mipmap.icon_flu);
                    weatherList.add(suggestion);
                    break;
                default:
                    break;
            }
        }

        for (Weather.DailyForecastEntity entity : weather.getDailyForecast()) {
            WeatherInfo futureWeather = new WeatherInfo();
            futureWeather.setItemType(WeatherInfo.TYPE_FUTURE_WEATHER);
            futureWeather.setWeatherIconRes(getWeatherIconResFromText(entity.getCondTxtD()));
            futureWeather.setMaxTmp(String.format("%s°", entity.getTmpMax()));
            futureWeather.setMinTmp(String.format("%s°", entity.getTmpMin()));
            futureWeather.setDescribe(String.format("%s。 最高%s℃。%s级%s %skm/h。 降水几率%s%%", entity.getCondTxtD(),
                    entity.getTmpMax(), entity.getWindSc(), entity.getWindDir(), entity.getWindSpd(), entity.getPop()));

            switch (TimeUtils.getFitTimeSpanByNow(entity.getDate(), "yyyy-MM-dd", TimeUtils.DAY)) {
                case "0天":
                    futureWeather.setTitle("今天");
                    break;
                case "1天":
                    futureWeather.setTitle("明天");
                    break;
                default:
                    futureWeather.setTitle(TimeUtils.getWeek(entity.getDate(), "yyyy-MM-dd"));
                    break;
            }
            weatherList.add(futureWeather);
        }
        return weatherList;
    }

    private int getWeatherIconResFromText(String text) {
        switch (text) {
            case "晴":
                return R.mipmap.type_two_sunny;
            case "阴":
                return R.mipmap.type_two_cloudy;
            case "多云":
                return R.mipmap.type_two_cloudy;
            case "少云":
                return R.mipmap.type_two_cloudy;
            case "晴间多云":
                return R.mipmap.type_two_cloudytosunny;
            case "小雨":
                return R.mipmap.type_two_light_rain;
            case "中雨":
                return R.mipmap.type_two_rain;
            case "大雨":
                return R.mipmap.type_two_rain;
            case "阵雨":
                return R.mipmap.type_two_rain;
            case "雷阵雨":
                return R.mipmap.type_two_thunderstorm;
            case "霾":
                return R.mipmap.type_two_haze;
            case "雾":
                return R.mipmap.type_two_fog;
            case "雨夹雪":
                return R.mipmap.type_two_snowrain;
            case "未知":
            default:
                return R.mipmap.none;
        }
    }

    @Override
    public void onDestroy() {
    }
}
