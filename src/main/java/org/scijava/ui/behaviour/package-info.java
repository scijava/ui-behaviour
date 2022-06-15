/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2022 Max Planck Institute of Molecular Cell Biology
 * and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
/**
 * <h1>Configurable-keys</h1> Simplify making AWT mouse-handlers configurable.
 *
 * Works along the lines of the InputMap / ActionMap mechanism. The syntax for
 * defining key and mouse "triggers" is described in the InputTrigger-syntax
 * wiki page <a href=
 * "https://github.com/scijava/ui-behaviour/wiki/InputTrigger-syntax">InputTrigger-syntax
 * wiki page</a>. They are also repeated below.
 *
 * <h2>Overview</h2>
 *
 * ui-behaviour is a library for binding behaviours by keys or mouse-actions.
 * The idea is similar to Swing's {@code InputMap/ActionMap} framework. The
 * difference is that actions are atomic while behaviours (possibly) stretch
 * over a period of time. For example, a {@code DragBehaviour} is initialized
 * (e.g., with a mouse click) at certain coordinates, goes through a series of
 * coordinate updates, and then ends (when the mouse is released).
 * <p>
 * The combination of modifiers, keys, mouse buttons etc. that initiates a
 * behaviour is called a "trigger" and is constructed from a string description.
 * <p>
 * The basic syntax for a trigger description is a sequence of modifier and key
 * names separated by whitespace. Examples are "{@code SPACE}",
 * "{@code button1}", "{@code shift alt scroll}", and "{@code ctrl F G}".
 * <p>
 * Additionally, one can specify a combination of modifiers, keys, mouse buttons
 * etc. that should be ignored when triggering the behaviour. This is a another
 * sequence of modifier and key names separated from the trigger description by
 * "{@code |}". For example, "{@code ctrl button1 | shift alt}" is triggered by
 * pressing the left mouse-button while holding the ctrl key. If the shift
 * and/or alt key are pressed simultaneously, the trigger still matches. To
 * ignore all other modifiers and keys, the special name "{@code all}" is used.
 * So, "{@code A | all}" is a trigger that matches when the A key is pressed,
 * regardless which other modifiers, keys, or buttons are active at the same
 * time.
 *
 *
 * <h2>Chaining principle</h2>
 *
 * In Swing, a {@code JComponent} has an {@code InputMap}. Each InputMap can
 * have a parent, and if a mapping for a given key is not found in the InputMap,
 * it asks the parent.
 * <p>
 * For BigDataViewer, we thought that this concept is nice for adding
 * InputMap/ActionMap pairs with related actions to the viewer. For example one
 * map with navigation shortcuts, then one map for bookmarking, and then each
 * user extension could also create its own InputMap/ActionMap pair and just
 * chain it to the existing maps.
 * <p>
 * The slight complication with this is, that if you have a situation like this:
 *
 * <pre>
 * component &rarr; map2 &rarr; map1
 * </pre>
 *
 * and you want to add another one, "map3", it should look like this:
 *
 * <pre>
 * component &rarr; map3 &rarr; map2 &rarr; map1
 * </pre>
 *
 * So, "map3" should be inserted between "component" and "map2".
 * <p>
 * This is resolved in ui-behaviours {@code InputActionBindings} by having a
 * {@code theInputMap/theActionMap} pair that acts as an empty leaf map with
 * exchangeable parents. From the component side it always looks like
 *
 * <pre>
 * component &rarr; theMap
 * </pre>
 *
 * and then internally the {@code InputActionBindings} maintains a list
 * {@code [map1, map2, ...]} which is then assembled into a new chain whenever
 * something changes. So
 *
 * <pre>
 * theMap &rarr; map2 &rarr; map1
 * </pre>
 *
 * becomes
 *
 * <pre>
 * theMap &rarr; map3 &rarr; map2 &rarr; map1
 * </pre>
 *
 * when "map3" is added.
 * <p>
 * And the behaviours framework doesn't have to care about the Swing particulars
 * (e.g., actually each {@code JComponent} has 3 different inputmaps for
 * different situations).
 *
 * <h2>Blocking maps</h2>
 *
 * On top of this, InputActionBindings adds a few convenience features.
 * <p>
 * For every map pair that you append to {@code InputActionBindings} you specify
 * a unique name that can later be used to remove the map pair again.
 * <p>
 * E.g., if map2 would have the name "bookmarking", then
 *
 * <pre>
 * InputActionBindings.removeInputMap( "bookmarking" )
 * </pre>
 *
 * would reassemble
 *
 * <pre>
 * theMap &rarr; map3 &rarr; map2 &rarr; map1
 * </pre>
 *
 * to
 *
 * <pre>
 * theMap &rarr; map3 &rarr; map1
 * </pre>
 *
 * Finally, given that all maps have unique names, when you add a map you can
 * specify a set of other maps to block.
 * <p>
 * For example, let's say that you want to temporarily add your own specific
 * keys that maybe conflict with the bookmarking keys.
 * <p>
 * Starting from this situation:
 *
 * <pre>
 * theMap &rarr; map3 &rarr; map2 &rarr; map1
 * </pre>
 *
 * the following
 *
 * <pre>
 * InputActionBindings.addInputMap( map4, "myMap", "navigation", "bookmarking" )
 * // "myMap" blocks "navigation", "bookmarking"
 * </pre>
 *
 * will result in
 *
 * <pre>
 * theMap &rarr; map4 &rarr; map3
 * </pre>
 *
 * that is, map1 and map2 were blocked.
 * <p>
 *
 * However, InputActionsBindings still knows about map1 and map2. Once the
 * blocking "myMap" is removed, they will be reinstated.
 * <p>
 * As a concrete example: this whole machinery is used for bookmarking in BDV.
 * You set a bookmark by pressing "shift B" and then some other key, that is the
 * name for the bookmark. For example, you can press "shift B" "1" to set a
 * bookmark named "1".
 * <p>
 * After you press "shift B", a temporary inputmap is installed that blocks all
 * other inputmaps. So "1" will not trigger switching to bdv source 1. After the
 * bookmark name "1" is received, the temporary inputmap is removed again.
 * <p>
 * For Behaviours, the chaining and blocking concepts are exactly the same
 *
 *
 *
 * <h2>Modifier names.</h2>
 *
 * The following modifiers can be used:
 *
 * <ul>
 * <li>ctrl
 * <li>alt
 * <li>altGraph
 * <li>shift
 * <li>meta
 * <li>win (the windows key)
 * <li>double-click (The trigger matches a double-click. This can include
 * "double-clicks" on keys, e.g., shift double-click A works as expected.)
 * </ul>
 *
 * <h2>Mouse buttons and scrolling.</h2>
 *
 * <ul>
 * <li>button1 (left mouse button)
 * <li>button2 (middle mouse button)
 * <li>button3 (right mouse button)
 * <li>scroll (The trigger matches scroll events (both horizontal and vertical).
 * Additional keys and modifiers may be present, e.g., shift A scroll matches
 * when scrolling while pressing the shift and A keys. This modifier can only be
 * used to trigger ScrollBehaviours.)
 * </ul>
 *
 *
 * <h2>Key names.</h2>
 *
 * Key names are the usual alphanumeric and function keys:
 *
 * <ul>
 * <li>A ... Z (Note that letter keys are always upper-case.)
 * <li>0 ... 9
 * <li>F1 ... F24
 * </ul>
 *
 * Moreover the following special key names are supported (as in the
 * AWTKeyStroke.getAWTKeyStroke(String) method):
 *
 * <ul>
 * <li>ENTER
 * <li>BACK_SPACE
 * <li>TAB
 * <li>CANCEL
 * <li>CLEAR
 * <li>COMPOSE
 * <li>PAUSE
 * <li>CAPS_LOCK
 * <li>ESCAPE
 * <li>SPACE
 * <li>PAGE_UP
 * <li>PAGE_DOWN
 * <li>END
 * <li>HOME
 * <li>BEGIN
 * <li>COMMA
 * <li>PERIOD
 * <li>SLASH
 * <li>SEMICOLON
 * <li>EQUALS
 * <li>OPEN_BRACKET
 * <li>BACK_SLASH
 * <li>CLOSE_BRACKET
 * </ul>
 *
 * These are names for cursor keys:
 *
 * <ul>
 * <li>LEFT
 * <li>UP
 * <li>RIGHT
 * <li>DOWN
 * </ul>
 *
 * These are names for the numpad keys:
 *
 * <ul>
 * <li>NUMPAD0 ... NUMPAD9
 * <li>MULTIPLY
 * <li>ADD
 * <li>SEPARATOR
 * <li>SUBTRACT
 * <li>DECIMAL
 * <li>DIVIDE
 * <li>DELETE
 * <li>NUM_LOCK
 * <li>SCROLL_LOCK
 * </ul>
 *
 */
package org.scijava.ui.behaviour;
