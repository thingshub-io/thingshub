package io.thingshub.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.queries.GeoHashCircleQuery;

/**
 * <p>
 * Geo工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class GeoUtil {

	private static String format = "0.000000";

	private static final double EARTH_RADIUS = 6371000;// 赤道半径(单位m)

	private static final int MAX_LEN = 12;

	/**
	 * 根据经纬度获得12位长度的Geohash字串
	 *
	 * @param lat 纬度
	 * @param lng 经度
	 * @return Geohash字串
	 */
	public static String encode(double lat, double lng) {
		return getGeoHash(lat, lng, MAX_LEN).toBase32();

	}

	/**
	 * 根据经纬度获得制定长度的Geohash字串
	 *
	 * @param lat    纬度值
	 * @param lng    经度值
	 * @param number 经度 1-12
	 * @return Geohash字串
	 */
	public static String encode(double lat, double lng, int number) {
		return getGeoHash(lat, lng, number).toBase32();

	}

	/**
	 * 获取整个九宫格的GeoHash的值
	 *
	 * @param lat 纬度值
	 * @param lng 经度值
	 * @return Geohash字串集合
	 */
	public static List<String> encodes(double lat, double lng) {
		List<String> hashs = new ArrayList<>();
		GeoHash[] adjacent = getGeoHash(lat, lng, MAX_LEN).getAdjacent();// 获取整个九宫格的GeoHash的值

		for (GeoHash hash : adjacent) {
			hashs.add(hash.toBase32());
		}

		return hashs;
	}

	/**
	 * 获取整个九宫格的GeoHash的值
	 *
	 * @param lat    纬度值
	 * @param lng    经度值
	 * @param number 经度 1-12
	 * @return Geohash字串集合
	 */
	public static List<String> encodes(double lat, double lng, int number) {
		List<String> hashs = new ArrayList<>();
		GeoHash[] adjacent = getGeoHash(lat, lng, number).getAdjacent();// 获取整个九宫格的GeoHash的值

		for (GeoHash hash : adjacent) {
			hashs.add(hash.toBase32());
		}

		return hashs;
	}

	/**
	 * 根据GeoHash的值转换为经纬度
	 * 
	 * @param geohash
	 * @return
	 */
	public static double[] decode(String geohash) {

		GeoHash geoHash = GeoHash.fromGeohashString(geohash);
		WGS84Point point = geoHash.getOriginatingPoint();
		double lat = point.getLatitude();
		double lng = point.getLongitude();

		DecimalFormat df = new DecimalFormat(format);

		return new double[] { Double.parseDouble(df.format(lat)), Double.parseDouble(df.format(lng)) };
	}

	/**
	 * 基于googleMap中的算法得到两经纬度之间的距离,计算精度与谷歌地图的距离精度差不多，相差范围在0.2米以下
	 * 
	 * @param lat1 第一点的精度
	 * @param lng1 第一点的纬度
	 * @param lat2 第二点的精度
	 * @param lng2 第二点的纬度
	 * @return 返回的距离，单位m
	 */

	public static double distance(double lat1, double lng1, double lat2, double lng2) {
		double x1 = Math.cos(lat1) * Math.cos(lng1);
		double y1 = Math.cos(lat1) * Math.sin(lng1);
		double z1 = Math.sin(lat1);
		double x2 = Math.cos(lat2) * Math.cos(lng2);
		double y2 = Math.cos(lat2) * Math.sin(lng2);
		double z2 = Math.sin(lat2);
		double lineDistance = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2));
		double s = EARTH_RADIUS * Math.PI * 2 * Math.asin(0.5 * lineDistance) / 180;
		return Math.round(s * 10000) / 10000;
	}

	private static GeoHash getGeoHash(double lat, double lng, int number) {
		DecimalFormat df = new DecimalFormat(format);
		return GeoHash.withCharacterPrecision(Double.parseDouble(df.format(lat)), Double.parseDouble(df.format(lng)), number);
	}

	public static void main(String[] args) {
		// 116.402843,39.999375 鸟巢 wx4g8c9v
		// 116.3967,39.99932 水立方 wx4g89tk
		// 116.40382,39.918118 故宫 wx4g0ffe
		double lon1 = 116.402843;
		double lat1 = 39.999375;
		double lon2 = 116.40382;
		double lat2 = 39.918118;
		double dist;
		String geocode;
		List<String> hashs = new ArrayList<>();

		dist = distance(lat1, lon1, lat2, lon2);
		System.out.println("两点相距1：" + dist + " 米");

		System.out.println("当前位置编码：" + encode(lat1, lon1));

		hashs = encodes(lat1, lon1);
		System.out.println("当前位置九宫格周围编码：" + hashs.toString());

		hashs = encodes(lat2, lon2);
		System.out.println("远方位置编码：" + hashs.toString());

		double[] decode = GeoUtil.decode(encode(lat1, lon1));
		System.out.println(decode[0] + "," + decode[1]);

		WGS84Point center = new WGS84Point(39.86391280373075, 116.37356590048701);
		GeoHashCircleQuery query = new GeoHashCircleQuery(center, 589);

		// the distance between center and test1 is about 430 meters
		WGS84Point test1 = new WGS84Point(39.8648866576058, 116.378465869303);
		// the distance between center and test2 is about 510 meters
		WGS84Point test2 = new WGS84Point(39.8664787092599, 116.378552856158);
		// the distance between center and test2 is about 600 meters
		WGS84Point test3 = new WGS84Point(39.8786787092599, 116.378552856158);

		assert query.contains(test1);
		assert query.contains(test2);
		assert query.contains(test3);

		System.out.println("================");

	}
}