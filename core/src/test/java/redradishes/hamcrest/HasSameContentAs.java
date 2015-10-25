package redradishes.hamcrest;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HasSameContentAs extends TypeSafeMatcher<CharSequence> {
  private final CharSequence charSequence;

  public HasSameContentAs(CharSequence charSequence) {
    this.charSequence = charSequence;
  }

  @Override
  protected boolean matchesSafely(CharSequence item) {
    return charSequence.equals(item.toString());
  }

  @Override
  public void describeTo(Description description) {
    description.appendValue(charSequence);
  }

  @Factory
  public static Matcher<CharSequence> hasSameContentAs(CharSequence expected) {
    return new HasSameContentAs(expected);
  }
}
