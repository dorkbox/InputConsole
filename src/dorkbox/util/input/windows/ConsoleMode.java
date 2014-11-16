/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
package dorkbox.util.input.windows;

/**
 * Console mode <p/> Constants copied <tt>wincon.h</tt>.
 */
public enum ConsoleMode {
  /**
   * The ReadFile or ReadConsole function returns only when a carriage return character is read. If this mode is
   * disable, the functions return when one or more characters are available.
   */
  ENABLE_LINE_INPUT(2),

  /**
   * Characters read by the ReadFile or ReadConsole function are written to the active screen buffer as they are read.
   * This mode can be used only if the ENABLE_LINE_INPUT mode is also enabled.
   */
  ENABLE_ECHO_INPUT(4),

  /**
   * CTRL+C is processed by the system and is not placed in the input buffer. If the input buffer is being read by
   * ReadFile or ReadConsole, other control keys are processed by the system and are not returned in the ReadFile or
   * ReadConsole buffer. If the ENABLE_LINE_INPUT mode is also enabled, backspace, carriage return, and linefeed
   * characters are handled by the system.
   */
  ENABLE_PROCESSED_INPUT(1),

  /**
   * User interactions that change the size of the console screen buffer are reported in the console's input buffee.
   * Information about these events can be read from the input buffer by applications using theReadConsoleInput
   * function, but not by those using ReadFile orReadConsole.
   */
  ENABLE_WINDOW_INPUT(8),

  /**
   * If the mouse pointer is within the borders of the console window and the window has the keyboard focus, mouse
   * events generated by mouse movement and button presses are placed in the input buffer. These events are discarded by
   * ReadFile or ReadConsole, even when this mode is enabled.
   */
  ENABLE_MOUSE_INPUT(16),;


  public final int code;

  ConsoleMode(final int code) {
    this.code = code;
  }
}
