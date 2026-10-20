package com.shopsphere.util;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
  private static final int ITERATIONS = 210000;

  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return ITERATIONS
        + ":"
        + Base64.getEncoder().encodeToString(salt)
        + ":"
        + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
  }

  private static byte[] derive(String p, byte[] salt, int n) {
    try {
      var spec = new PBEKeySpec(p.toCharArray(), salt, n, 256);
      try {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .getEncoded();
      } finally {
        spec.clearPassword();
      }
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean verify(String p, String encoded) {
    try {
      var parts = encoded.split(":");
      return MessageDigest.isEqual(
          Base64.getDecoder().decode(parts[2]),
          derive(p, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
    } catch (RuntimeException e) {
      return false;
    }
  }
}
