package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 A list of {@link Element}s, with methods that act on every element in the list.
 <p>To get an {@code Elements} object, use the {@link Element#select(String)} method.</p>
 <p>Methods that {@link #set(int, Element) set}, {@link #remove(int) remove}, or {@link #replaceAll(UnaryOperator)
 replace} Elements in the list will also act on the underlying {@link org.jsoup.nodes.Document DOM}.</p>

 @author Jonathan Hedley, jonathan@hedley.net */
public class Elements extends Nodes<Element> {
    public Elements() {
    }

    public Elements(int initialCapacity) {
        super(initialCapacity);
    }

    public Elements(Collection<Element> elements) {
        super(elements);
    }

    public Elements(List<Element> elements) {
        super(elements);
    }

    public Elements(Element... elements) {
    	super(Arrays.asList(elements));
    }

    /**
     * Creates a deep copy of these elements.
     * @return a deep copy
     */
    @Override
    public Elements clone() {
        Elements clone = new Elements(size());
        for (Element e : this)
            clone.add(e.clone());
        return clone;
    }

    /**
     Convenience method to get the Elements as a plain ArrayList. This allows modification to the list of elements
     without modifying the source Document. I.e. whereas calling {@code elements.remove(0)} will remove the element from
     both the Elements and the DOM, {@code elements.asList().remove(0)} will remove the element from the list only.
     <p>Each Element is still the same DOM connected Element.</p>

     @return a new ArrayList containing the elements in this list
     @since 1.19.2
     @see #Elements(List)
     */
    @Override
    public ArrayList<Element> asList() {
        return new ArrayList<>(this);
    }

    // attribute methods
    /**
     Get an attribute value from the first matched element that has the attribute.
     @param attributeKey The attribute key.
     @return The attribute value from the first matched element that has the attribute. If no elements were matched (isEmpty() == true),
     or if the no elements have the attribute, returns empty string.
     @see #hasAttr(String)
     */
    public String attr(String attributeKey) {
        for (Element element : this) {
            if (element.hasAttr(attributeKey))
                return element.attr(attributeKey);
        }
        return "";
    }

    /**
     Checks if any of the matched elements have this attribute defined.
     @param attributeKey attribute key
     @return true if any of the elements have the attribute; false if none do.
     */
    public boolean hasAttr(String attributeKey) {
        for (Element element : this) {
            if (element.hasAttr(attributeKey))
                return true;
        }
        return false;
    }

    /**
     * Get the attribute value for each of the matched elements. If an element does not have this attribute, no value is
     * included in the result set for that element.
     * @param attributeKey the attribute name to return values for. You can add the {@code abs:} prefix to the key to
     * get absolute URLs from relative URLs, e.g.: {@code doc.select("a").eachAttr("abs:href")} .
     * @return a list of each element's attribute value for the attribute
     */
    public List<String> eachAttr(String attributeKey) {
        List<String> attrs = new ArrayList<>(size());
        for (Element element : this) {
            if (element.hasAttr(attributeKey))
                attrs.add(element.attr(attributeKey));
        }
        return attrs;
    }

    /**
     * Set an attribute on all matched elements.
     * @param attributeKey attribute key
     * @param attributeValue attribute value
     * @return this
     */
    public Elements attr(String attributeKey, String attributeValue) {
        for (Element element : this) {
            element.attr(attributeKey, attributeValue);
        }
        return this;
    }

    /**
     * Remove an attribute from every matched element.
     * @param attributeKey The attribute to remove.
     * @return this (for chaining)
     */
    public Elements removeAttr(String attributeKey) {
        for (Element element : this) {
            element.removeAttr(attributeKey);
        }
        return this;
    }

    /**
     Add the class name to every matched element's {@code class} attribute.
     @param className class name to add
     @return this
     */
    public Elements addClass(String className) {
        for (Element element : this) {
            element.addClass(className);
        }
        return this;
    }

    /**
     Remove the class name from every matched element's {@code class} attribute, if present.
     @param className class name to remove
     @return this
     */
    public Elements removeClass(String className) {
        for (Element element : this) {
            element.removeClass(className);
        }
        return this;
    }

    /**
     Toggle the class name on every matched element's {@code class} attribute.
     @param className class name to add if missing, or remove if present, from every element.
     @return this
     */
    public Elements toggleClass(String className) {
        for (Element element : this) {
            element.toggleClass(className);
        }
        return this;
    }

    /**
     Determine if any of the matched elements have this class name set in their {@code class} attribute.
     @param className class name to check for
     @return true if any do, false if none do
     */
    public boolean hasClass(String className) {
        for (Element element : this) {
            if (element.hasClass(className))
                return true;
        }
        return false;
    }
    
    /**
     * Get the form element's value of the first matched element.
     * @return The form element's value, or empty if not set.
     * @see Element#val()
     */
    public String val() {
        if (size() > 0)
            //noinspection ConstantConditions
            return first().val(); // first() != null as size() > 0
        else
            return "";
    }
    
    /**
     * Set the form element's value in each of the matched elements.
     * @param value The value to set into each matched element
     * @return this (for chaining)
     */
    public Elements val(String value) {
        for (Element element : this)
            element.val(value);
        return this;
    }
    
    /**
     * Get the combined text of all the matched elements.
     * <p>
     * Note that it is possible to get repeats if the matched elements contain both parent elements and their own
     * children, as the Element.text() method returns the combined text of a parent and all its children.
     * @return string of all text: unescaped and no HTML.
     * @see Element#text()
     * @see #eachText()
     */
    public String text() {
        return stream()
            .map(Element::text)
            .collect(StringUtil.joining(" "));
    }

    /**
     * Get the combined original text of all the matched elements.
     * <p>
     * Note that it is possible to get repeats if the matched elements contain both parent elements and their own
     * children, as the Element.wholeText() method returns the combined original text of a parent and all its children.
     * @return string of all text: unescaped and no HTML.
     * @see Element#wholeText()
     * @see #eachWholeText()
     */
    public String wholeText() {
        StringBuilder sb = StringUtil.borrowBuilder();
        for (Element element : this) {
            if (sb.length() != 0)
                sb.append("\n");
            sb.append(element.wholeText());
        }
        return StringUtil.releaseBuilder(sb);
    }

    /**
     Test if any matched Element has any text content, that is not just whitespace.
     @return true if any element has non-blank text content.
     @see Element#hasText()
     */
    public boolean hasText() {
        for (Element element: this) {
            if (element.hasText())
                return true;
        }
        return false;
    }

    /**
     * Get the text content of each of the matched elements. If an element has no text, then it is not included in the
     * result.
     * @return A list of each matched element's text content.
     * @see Element#text()
     * @see Element#hasText()
     * @see #text()
     */
    public List<String> eachText() {
        ArrayList<String> texts = new ArrayList<>(size());
        for (Element el: this) {
            if (el.hasText())
                texts.add(el.text());
        }
        return texts;
    }

    /**
     * Get the text content of each of the matched elements. If an element has no text, then it is not included in the
     * result.
     * @return A list of each matched element's original text content.
     * @see Element#wholeText()
     * @see Element#hasText()
     * @see #text()
     */
    public List<String> eachWholeText() {
        ArrayList<String> texts = new ArrayList<>(size());
        for (Element el: this) {
            if (el.hasText())
                texts.add(el.wholeText());
        }
        return texts;
    }
    
    /**
     * Get the combined inner HTML of all matched elements.
     * @return string of all element's inner HTML.
     * @see #text()
     * @see #outerHtml()
     */
    public String html() {
        return stream()
            .map(Element::html)
            .collect(StringUtil.joining("\n"));
    }

    /**
     * Update (rename) the tag name of each matched element. For example, to change each {@code <i>} to a {@code <em>}, do
     * {@code doc.select("i").tagName("em");}
     *
     * @param tagName the new tag name
     * @return this, for chaining
     * @see Element#tagName(String)
     */
    public Elements tagName(String tagName) {
        for (Element element : this) {
            element.tagName(tagName);
        }
        return this;
    }
    
    /**
     * Set the inner HTML of each matched element.
     * @param html HTML to parse and set into each matched element.
     * @return this, for chaining
     * @see Element#html(String)
     */
    public Elements html(String html) {
        for (Element element : this) {
            element.html(html);
        }
        return this;
    }
    
    /**
     * Add the supplied HTML to the start of each matched element's inner HTML.
     * @param html HTML to add inside each element, before the existing HTML
     * @return this, for chaining
     * @see Element#prepend(String)
     */
    public Elements prepend(String html) {
        for (Element element : this) {
            element.prepend(html);
        }
        return this;
    }
    
    /**
     * Add the supplied HTML to the end of each matched element's inner HTML.
     * @param html HTML to add inside each element, after the existing HTML
     * @return this, for chaining
     * @see Element#append(String)
     */
    public Elements append(String html) {
        for (Element element : this) {
            element.append(html);
        }
        return this;
    }

    /**
     Insert the supplied HTML before each matched element's outer HTML.

     @param html HTML to insert before each element
     @return this, for chaining
     @see Element#before(String)
     */
    @Override
    public Elements before(String html) {
        super.before(html);
        return this;
    }

    /**
     Insert the supplied HTML after each matched element's outer HTML.

     @param html HTML to insert after each element
     @return this, for chaining
     @see Element#after(String)
     */
    @Override
    public Elements after(String html) {
        super.after(html);
        return this;
    }

    /**
     Wrap the supplied HTML around each matched elements. For example, with HTML
     {@code <p><b>This</b> is <b>Jsoup</b></p>},
     <code>doc.select("b").wrap("&lt;i&gt;&lt;/i&gt;");</code>
     becomes {@code <p><i><b>This</b></i> is <i><b>jsoup</b></i></p>}

     @param html HTML to wrap around each element, e.g. {@code <div class="head"></div>}. Can be arbitrarily deep.
     @return this (for chaining)
     @see Element#wrap
     */
    @Override
    public Elements wrap(String html) {
        super.wrap(html);
        return this;
    }

    /**
     * Removes the matched elements from the DOM, and moves their children up into their parents. This has the effect of
     * dropping the elements but keeping their children.
     * <p>
     * This is useful for e.g removing unwanted formatting elements but keeping their contents.
     * </p>
     * 
     * E.g. with HTML: <p>{@code <div><font>One</font> <font><a href="/">Two</a></font></div>}</p>
     * <p>{@code doc.select("font").unwrap();}</p>
     * <p>HTML = {@code <div>One <a href="/">Two</a></div>}</p>
     *
     * @return this (for chaining)
     * @see Node#unwrap
     */
    public Elements unwrap() {
        for (Element element : this) {
            element.unwrap();
        }
        return this;
    }

    /**
     * Empty (remove all child nodes from) each matched element. This is similar to setting the inner HTML of each
     * element to nothing.
     * <p>
     * E.g. HTML: {@code <div><p>Hello <b>there</b></p> <p>now</p></div>}<br>
     * <code>doc.select("p").empty();</code><br>
     * HTML = {@code <div><p></p> <p></p></div>}
     * @return this, for chaining
     * @see Element#empty()
     * @see #remove()
     */
    public Elements empty() {
        for (Element element : this) {
            element.empty();
        }
        return this;
    }

    /**
     * Remove each matched element from the DOM. This is similar to setting the outer HTML of each element to nothing.
     * <p>The elements will still be retained in this list, in case further processing of them is desired.</p>
     * <p>
     * E.g. HTML: {@code <div><p>Hello</p> <p>there</p> <img /></div>}<br>
     * <code>doc.select("p").remove();</code><br>
     * HTML = {@code <div> <img /></div>}
     * <p>
     * Note that this method should not be used to clean user-submitted HTML; rather, use {@link org.jsoup.safety.Cleaner} to clean HTML.
     * @return this, for chaining
     * @see Element#empty()
     * @see #empty()
     * @see #clear()
     */
    @Override
    public Elements remove() {
        super.remove();
        return this;
    }
    
    // filters
    
    /**
     * Find matching elements within this element list.
     * @param query A {@link Selector} query
     * @return the filtered list of elements, or an empty list if none match.
     */
    public Elements select(String query) {
        return Selector.select(query, this);
    }

    /**
     Find the first Element that matches the {@link Selector} CSS query within this element list.
     <p>This is effectively the same as calling {@code elements.select(query).first()}, but is more efficient as query
     execution stops on the first hit.</p>

     @param cssQuery a {@link Selector} query
     @return the first matching element, or <b>{@code null}</b> if there is no match.
     @see #expectFirst(String)
     @since 1.19.1
     */
    public @Nullable Element selectFirst(String cssQuery) {
        return Selector.selectFirst(cssQuery, this);
    }

    /**
     Just like {@link #selectFirst(String)}, but if there is no match, throws an {@link IllegalArgumentException}.

     @param cssQuery a {@link Selector} query
     @return the first matching element
     @throws IllegalArgumentException if no match is found
     @since 1.19.1
     */
    public Element expectFirst(String cssQuery) {
        return Validate.expectNotNull(
            Selector.selectFirst(cssQuery, this),
            "No elements matched the query '%s' in the elements.", cssQuery
        );
    }

    /**
     * Remove elements from this list that match the {@link Selector} query.
     * <p>
     * E.g. HTML: {@code <div class=logo>One</div> <div>Two</div>}<br>
     * <code>Elements divs = doc.select("div").not(".logo");</code><br>
     * Result: {@code divs: [<div>Two</div>]}
     * <p>
     * @param query the selector query whose results should be removed from these elements
     * @return a new elements list that contains only the filtered results
     */
    public Elements not(String query) {
        Elements out = Selector.select(query, this);
        return Selector.filterOut(this, out);
    }
    
    /**
     * Get the <i>nth</i> matched element as an Elements object.
     * <p>
     * See also {@link #get(int)} to retrieve an Element.
     * @param index the (zero-based) index of the element in the list to retain
     * @return Elements containing only the specified element, or, if that element did not exist, an empty list.
     */
    public Elements eq(int index) {
        return size() > index ? new Elements(get(index)) : new Elements();
    }
    
    /**
     * Test if any of the matched elements match the supplied query.
     * @param query A selector
     * @return true if at least one element in the list matches the query.
     */
    public boolean is(String query) {
        Evaluator eval = Selector.evaluatorOf(query);
        for (Element e : this) {
            if (e.is(eval))
                return true;
        }
        return false;
    }

    /**
     * Get the immediate next element sibling of each element in this list.
     * @return next element siblings.
     */
    public Elements next() {
        return siblings(null, true, false);
    }

    /**
     * Get the immediate next element sibling of each element in this list, filtered by the query.
     * @param query CSS query to match siblings against
     * @return next element siblings.
     */
    public Elements next(String query) {
        return siblings(query, true, false);
    }

    /**
     * Get each of the following element siblings of each element in this list.
     * @return all following element siblings.
     */
    public Elements nextAll() {
        return siblings(null, true, true);
    }

    /**
     * Get each of the following element siblings of each element in this list, that match the query.
     * @param query CSS query to match siblings against
     * @return all following element siblings.
     */
    public Elements nextAll(String query) {
        return siblings(query, true, true);
    }

    /**
     * Get the immediate previous element sibling of each element in this list.
     * @return previous element siblings.
     */
    public Elements prev() {
        return siblings(null, false, false);
    }

    /**
     * Get the immediate previous element sibling of each element in this list, filtered by the query.
     * @param query CSS query to match siblings against
     * @return previous element siblings.
     */
    public Elements prev(String query) {
        return siblings(query, false, false);
    }

    /**
     * Get each of the previous element siblings of each element in this list.
     * @return all previous element siblings.
     */
    public Elements prevAll() {
        return siblings(null, false, true);
    }

    /**
     * Get each of the previous element siblings of each element in this list, that match the query.
     * @param query CSS query to match siblings against
     * @return all previous element siblings.
     */
    public Elements prevAll(String query) {
        return siblings(query, false, true);
    }

    private Elements siblings(@Nullable String query, boolean next, boolean all) {
        Elements els = new Elements();
        Evaluator eval = query != null? Selector.evaluatorOf(query) : null;
        for (Element e : this) {
            do {
                Element sib = next ? e.nextElementSibling() : e.previousElementSibling();
                if (sib == null) break;
                if (eval == null || sib.is(eval)) els.add(sib);
                e = sib;
            } while (all);
        }
        return els;
    }

    /**
     * Get all of the parents and ancestor elements of the matched elements.
     * @return all of the parents and ancestor elements of the matched elements
     */
    public Elements parents() {
        HashSet<Element> combo = new LinkedHashSet<>();
        for (Element e: this) {
            combo.addAll(e.parents());
        }
        return new Elements(combo);
    }

    // list-like methods
    /**
     Get the first matched element.
     @return The first matched element, or <code>null</code> if contents is empty.
     */
    @Override
    public @Nullable Element first() {
        return super.first();
    }

    /**
     Get the last matched element.
     @return The last matched element, or <code>null</code> if contents is empty.
     */
    @Override
    public @Nullable Element last() {
        return super.last();
    }

    /**
     * Perform a depth-first traversal on each of the selected elements.
     * @param nodeVisitor the visitor callbacks to perform on each node
     * @return this, for chaining
     */
    public Elements traverse(NodeVisitor nodeVisitor) {
        NodeTraversor.traverse(nodeVisitor, this);
        return this;
    }

    /**
     * Perform a depth-first filtering on each of the selected elements.
     * @param nodeFilter the filter callbacks to perform on each node
     * @return this, for chaining
     */
    public Elements filter(NodeFilter nodeFilter) {
        NodeTraversor.filter(nodeFilter, this);
        return this;
    }

    /**
     * Get the {@link FormElement} forms from the selected elements, if any.
     * @return a list of {@link FormElement}s pulled from the matched elements. The list will be empty if the elements contain
     * no forms.
     */
    public List<FormElement> forms() {
        ArrayList<FormElement> forms = new ArrayList<>();
        for (Element el: this)
            if (el instanceof FormElement)
                forms.add((FormElement) el);
        return forms;
    }

    /**
     * Get {@link Comment} nodes that are direct child nodes of the selected elements.
     * @return Comment nodes, or an empty list if none.
     */
    public List<Comment> comments() {
        return childNodesOfType(Comment.class);
    }

    /**
     * Get {@link TextNode} nodes that are direct child nodes of the selected elements.
     * @return TextNode nodes, or an empty list if none.
     */
    public List<TextNode> textNodes() {
        return childNodesOfType(TextNode.class);
    }

    /**
     * Get {@link DataNode} nodes that are direct child nodes of the selected elements. DataNode nodes contain the
     * content of tags such as {@code script}, {@code style} etc and are distinct from {@link TextNode}s.
     * @return Comment nodes, or an empty list if none.
     */
    public List<DataNode> dataNodes() {
        return childNodesOfType(DataNode.class);
    }

    private <T extends Node> List<T> childNodesOfType(Class<T> tClass) {
        ArrayList<T> nodes = new ArrayList<>();
        for (Element el: this) {
            for (int i = 0; i < el.childNodeSize(); i++) {
                Node node = el.childNode(i);
                if (tClass.isInstance(node))
                    nodes.add(tClass.cast(node));
            }
        }
        return nodes;
    }

    // list methods that update the DOM:

    /**
     Replace the Element at the specified index in this list, and in the DOM.

     @param index index of the element to replace
     @param element element to be stored at the specified position
     @return the old Element at this index
     @since 1.17.1
     */
    @Override
    public Element set(int index, Element element) {
        return super.set(index, element);
    }

    /**
     Remove the Element at the specified index in this ist, and from the DOM.

     @param index the index of the element to be removed
     @return the old element at this index
     @see #deselect(int)
     @since 1.17.1
     */
    @Override
    public Element remove(int index) {
        return super.remove(index);
    }


    /**
     Remove the Element at the specified index in this list, but not from the DOM.

     @param index the index of the element to be removed
     @return the old element at this index
     @see #remove(int)
     @since 1.19.2
     */
    @Override
    public Element deselect(int index) {
        return super.deselect(index);
    }
}
