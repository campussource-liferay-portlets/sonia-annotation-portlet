package sonia.annotation.portlet;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.base.Strings;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

/**
 *
 * @author th
 */
public class SoniaPortletPreferencesHandler
{

  /** Field description */
  private static final Log LOGGER =
    LogFactoryUtil.getLog(SoniaPortletPreferencesHandler.class);

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   */
  private SoniaPortletPreferencesHandler() {}

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   *
   * @param preferences
   * @param object
   */
  public static void load(PortletPreferences preferences, Object object)
  {
    Class objectClass = object.getClass();

    LOGGER.debug("SoniaPortletPropertyHandler:load "
      + objectClass.getCanonicalName());

    Field[] fields = objectClass.getDeclaredFields();

    for (Field f : fields)
    {
      if (f.isAnnotationPresent(SoniaPortletPreference.class))
      {
        Class fieldType = f.getType();
        String fieldName = f.getName();
        SoniaPortletPreference annotation =
          (SoniaPortletPreference) f.getAnnotation(
          SoniaPortletPreference.class);

        LOGGER.debug("field name = " + f.getName() + " / "
          + fieldType.getCanonicalName());

        String setterName = "set" + fieldName.substring(0, 1).toUpperCase()
                            + fieldName.substring(1);
        String propertyName = objectClass.getCanonicalName() + "_"
                              + f.getName();

        try
        {
          Method setter = objectClass.getMethod(setterName, fieldType);

          LOGGER.debug("  property name = " + propertyName);

          String value = preferences.getValue(propertyName, null);

          LOGGER.debug("  value = " + value);

          if (fieldType == java.lang.String.class)
          {
            String defaultValue = annotation.value();

            if (Strings.isNullOrEmpty(value)
              &&!Strings.isNullOrEmpty(defaultValue))
            {
              value = defaultValue;
            }

            setter.invoke(object, value);
          }
          else if (fieldType == int.class)
          {
            if (Strings.isNullOrEmpty(value))
            {
              String defaultValue = annotation.value();
              int v = 0;

              if (!Strings.isNullOrEmpty(defaultValue))
              {
                v = Integer.parseInt(defaultValue);
              }

              setter.invoke(object, v);
            }
            else
            {
              setter.invoke(object, Integer.parseInt(value));
            }
          }
          else if (fieldType == boolean.class)
          {
            boolean v;

            if (Strings.isNullOrEmpty(value))
            {
              v = "true".equals(annotation.value());
            }
            else
            {
              v = "true".equals(value);
            }

            setter.invoke(object, v);
          }
        }
        catch (NoSuchMethodException | SecurityException ex)
        {
          LOGGER.error("getter method failed", ex);
        }
        catch (IllegalAccessException | IllegalArgumentException
          | InvocationTargetException ex)
        {
          LOGGER.error("set value failed", ex);
        }

      }
    }
  }

  /**
   * Method description
   *
   *
   *
   * @param preferences
   * @param object
   */
  public static void store(PortletPreferences preferences, Object object)
  {
    Class objectClass = object.getClass();

    LOGGER.debug("SoniaPortletPropertyHandler:store "
      + objectClass.getCanonicalName());

    Field[] fields = objectClass.getDeclaredFields();

    for (Field f : fields)
    {
      if (f.isAnnotationPresent(SoniaPortletPreference.class))
      {
        Class fieldType = f.getType();
        String fieldName = f.getName();

        LOGGER.debug("field name = " + f.getName() + " / "
          + fieldType.getCanonicalName());

        String getterName = ((fieldType == boolean.class)
          ? "is"
          : "get") + fieldName.substring(0, 1).toUpperCase()
            + fieldName.substring(1);
        String propertyName = objectClass.getCanonicalName() + "_"
                              + f.getName();

        try
        {
          Method getter = objectClass.getMethod(getterName);

          LOGGER.debug("  getter name = " + getterName);
          LOGGER.debug("  property name = " + propertyName);

          String value = "";

          if (fieldType == java.lang.String.class)
          {
            value = (String) getter.invoke(object);
          }
          else if (fieldType == int.class)
          {
            value = Integer.toString((int) getter.invoke(object));
          }
          else if (fieldType.equals(boolean.class))
          {
            value = Boolean.toString((boolean) getter.invoke(object));
          }

          LOGGER.debug("  value = " + value);

          preferences.setValue(propertyName, value);
        }
        catch (NoSuchMethodException | SecurityException ex)
        {
          LOGGER.error("getter method failed", ex);
        }
        catch (IllegalAccessException | IllegalArgumentException
          | InvocationTargetException ex)
        {
          LOGGER.error("get value failed", ex);
        }
        catch (ReadOnlyException ex)
        {
          LOGGER.error("store read only value", ex);
        }
      }
    }

    try
    {
      preferences.store();
    }
    catch (IOException | ValidatorException ex)
    {
      LOGGER.error("store preferences failed", ex);
    }
  }
}
