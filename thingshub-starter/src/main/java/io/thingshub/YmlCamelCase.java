package io.thingshub;

public class YmlCamelCase {

	public static String camelize(String input) {
		for (int i = 0; i < input.length(); i++) {
			if (input.substring(i, i + 1).equals("-")) {
				input.replace("-", "");
				input = input.substring(0, i) + input.substring(i + 1, i + 2).toUpperCase() + input.substring(i + 2);
			}
			if (input.substring(i, i + 1).equals(" ")) {
				input.replace(" ", "");
				input = input.substring(0, i) + input.substring(i + 1, i + 2).toUpperCase() + input.substring(i + 2);
			}
		}

		return input;
	}
}