import streamlit as st
from streamlit_autorefresh import st_autorefresh
import json
import pandas as pd
from collections import defaultdict
import networkx as nx
from pyvis.network import Network
import streamlit.components.v1 as components
import re  # For regular expressions
import os
import hashlib

# Set up the page with custom CSS
st.set_page_config(page_title="Temporal / Apache Pulsar Simulation", layout="wide")

# Add custom CSS to remove extra padding and handle dark mode
st.markdown(
    """
    <style>
        /* Remove extra padding at the top */
        .block-container {
            padding-top: 1rem !important;
        }
        
        /* Handle dark mode for network graph container */
        [data-theme="dark"] iframe {
            background-color: transparent !important;
        }
        
        /* Adjust iframe background in both light/dark modes */
        iframe {
            background-color: transparent !important;
            margin-bottom: 2rem !important;
        }
        
        /* Customize scrollbar */
        #message-container::-webkit-scrollbar {
            width: 10px;
        }
        
        #message-container::-webkit-scrollbar-track {
            background: #f1f1f1;
        }
        
        #message-container::-webkit-scrollbar-thumb {
            background: #888;
            border-radius: 5px;
        }
        
        #message-container::-webkit-scrollbar-thumb:hover {
            background: #555;
        }
    </style>
""",
    unsafe_allow_html=True,
)

# Auto-refresh the app every 2 seconds
st_autorefresh(interval=2 * 1000, key="datarefresh")


# Add function to determine node color based on patterns
def get_node_color(node_id):
    node_id_lower = node_id.lower()

    if "handler" in node_id_lower:
        return "#FFB6C1"  # Light pink
    elif node_id.endswith("Workflow"):
        return "#B8E6B8"  # Light green
    elif "activity" in node_id_lower:
        return "#ADD8E6"  # Light blue
    elif "worker" in node_id_lower:
        return "#FFE4B5"  # Light peach

    return "#E8E8E8"  # Default light gray for nodes that don't match any pattern


st.title("Apache Pulsar Simulation")


# Function to add border to iframe in the generated HTML
def add_border_to_iframe(html):
    # Use regex to find the iframe tag
    pattern = r"(<iframe.*?)(>.*<\/iframe>)"
    match = re.search(pattern, html, re.DOTALL)
    if match:
        iframe_start = match.group(1)
        iframe_end = match.group(2)
        # Check if 'style' attribute exists
        if 'style="' in iframe_start:
            # Add border style to existing style attribute
            iframe_start = re.sub(
                r'style="(.*?)"', r'style="\1; border:1px solid black;"', iframe_start
            )
        else:
            # Add new style attribute with border
            iframe_start += ' style="border:1px solid black;"'
        new_iframe_tag = iframe_start + iframe_end
        html = html.replace(match.group(0), new_iframe_tag)
    return html


# Function to load data with caching
@st.cache_data(ttl=2)
def load_data(data):
    # Use the provided data instead of reading from the file
    # Split the data into lines and parse each line as JSON
    lines = data.strip().split("\n")
    records = []
    for idx, line in enumerate(lines):
        line = line.strip()
        if not line:
            continue
        try:
            record = json.loads(line)
            records.append(record)
        except json.JSONDecodeError as e:
            st.error(f"Error parsing line {idx+1}: {line}\n{e}")
            continue

    if not records:
        st.warning("No valid JSON records found.")
        return None, None

    df = pd.DataFrame(records)
    return df, records


# Function to generate message HTML
def generate_message_html(message_df):
    # Style the DataFrame using Pandas Styler and render it to HTML
    message_df_style = message_df.style.set_table_styles(
        [
            {
                "selector": "th",
                "props": [
                    ("background-color", "#f0f0f0"),
                    (
                        "font-family",
                        "'Lucida Console', 'Monaco', 'Courier New', monospace",
                    ),
                    ("font-weight", "normal"),
                    ("font-size", "12px"),
                ],
            },
            {
                "selector": "td",
                "props": [
                    ("border", "1px solid #ddd"),
                    ("padding", "8px"),
                    (
                        "font-family",
                        "'Lucida Console', 'Monaco', 'Courier New', monospace",
                    ),
                    ("font-size", "12px"),
                ],
            },
            {
                "selector": "tr:nth-child(even)",
                "props": [("background-color", "#f9f9f9")],
            },
            {"selector": "tr:hover", "props": [("background-color", "#f1f1f1")]},
            {
                "selector": "table",
                "props": [("border-collapse", "collapse"), ("width", "100%")],
            },
        ]
    )

    message_df_html = message_df_style.to_html()

    # Create HTML with auto-scroll functionality using localStorage
    message_html = f"""
    <div id="message-container" style="height:300px; overflow-y:scroll; border:1px solid #ddd;">
        {message_df_html}
    </div>

    <script type="text/javascript">
        // Your JavaScript code remains unchanged
        const messageContainer = document.getElementById('message-container');

        // Retrieve shouldAutoScroll from localStorage
        let shouldAutoScroll = localStorage.getItem('shouldAutoScroll');
        if (shouldAutoScroll === null) {{
            shouldAutoScroll = true;
        }} else {{
            shouldAutoScroll = JSON.parse(shouldAutoScroll);
        }}

        // Scroll position restoration
        const storedScrollPosition = localStorage.getItem('messageContainerScrollTop');
        if (storedScrollPosition !== null) {{
            messageContainer.scrollTop = parseInt(storedScrollPosition);
        }}

        messageContainer.addEventListener('scroll', function() {{
            const scrollPosition = messageContainer.scrollTop + messageContainer.clientHeight;
            const scrollHeight = messageContainer.scrollHeight;

            // Save the current scrollTop to localStorage
            localStorage.setItem('messageContainerScrollTop', messageContainer.scrollTop);

            // Check if the user is at the bottom (allow a small margin for floating point errors)
            if(scrollHeight - scrollPosition > 5) {{
                shouldAutoScroll = false;
            }} else {{
                shouldAutoScroll = true;
            }}
            // Save shouldAutoScroll to localStorage
            localStorage.setItem('shouldAutoScroll', JSON.stringify(shouldAutoScroll));
        }});

        function scrollToBottom() {{
            if(shouldAutoScroll) {{
                messageContainer.scrollTop = messageContainer.scrollHeight;
                // Update the scroll position in localStorage
                localStorage.setItem('messageContainerScrollTop', messageContainer.scrollTop);
            }}
        }}

        scrollToBottom();
    </script>
    """

    return message_html


# Function to safely read a file
def read_file_content(file_path):
    try:
        with open(file_path, "r", encoding="utf-8") as file:
            return file.read()
    except FileNotFoundError:
        st.warning(f"File not found: {file_path}")
        return ""


# Load data
flow_data = read_file_content("./logs/flow.json")
php_flow_data = read_file_content("./logs/php-flow.json")

# Combine the data (if both files have content, join with newline)
combined_data = "\n".join(filter(None, [flow_data, php_flow_data]))

# Pass the combined file content to the load_data function
df, records = load_data(combined_data)
if df is not None and records is not None:
    # Section 1: Display messages
    st.header("Messages")
    message_df = df[df["log_type"] == "message"][["timestamp", "data"]]
    message_df = message_df.reset_index(drop=True)

    # Calculate a hash of the current messages data
    current_hash = hashlib.md5(message_df.to_json().encode()).hexdigest()

    # Check if the hash is different from the stored hash
    if (
        "messages_hash" not in st.session_state
        or st.session_state.messages_hash != current_hash
    ):
        # Data has changed; update the stored hash
        st.session_state.messages_hash = current_hash

        # Generate the message HTML
        message_html = generate_message_html(message_df)

        # Store the generated HTML in session state
        st.session_state.message_html = message_html
    else:
        # Data hasn't changed; use the stored HTML
        message_html = st.session_state.message_html

    # Display the HTML in Streamlit with specified width
    components.html(message_html, height=300, scrolling=False, width=1200)

    # Section 2: Topics Graph Visualization
    st.header("Topics Graph")

    # Prepare data for nodes and edges
    node_counts = defaultdict(int)
    edge_counts = defaultdict(int)
    nodes_in_edges = set()

    # Process 'publish' events to build edges and collect nodes involved
    for record in records:
        if record.get("log_type") == "publish":
            data_field = record.get("data", {})
            from_node = data_field.get("node")
            to_node = data_field.get("target_node")

            if from_node and to_node:
                edge_counts[(from_node, to_node)] += 1
                nodes_in_edges.update([from_node, to_node])

    # Process 'receive' events to count messages received at nodes
    for record in records:
        if record.get("log_type") == "receive":
            data_field = record.get("data", {})
            node_name = data_field.get("node_name")
            if node_name:
                node_counts[node_name] += 1

    # Ensure all nodes involved in edges are in node_counts
    for node in nodes_in_edges:
        if node not in node_counts:
            node_counts[node] = 0

    # Create a NetworkX graph
    G = nx.DiGraph()

    # Add nodes
    for node, count in node_counts.items():
        G.add_node(node, count=count)

    # Add edges
    for (from_node, to_node), count in edge_counts.items():
        G.add_edge(from_node, to_node, count=count)

    # Generate network graph using PyVis
    net = Network(height="800px", width="100%", directed=True)

    # Adjust hierarchical layout options and physics settings
    net.set_options(
        """
        var options = {
        "layout": {
            "hierarchical": {
            "enabled": true,
            "direction": "UD",
            "sortMethod": "directed",
            "levelSeparation": 200,
            "nodeSpacing": 200,
            "treeSpacing": 200,
            "blockShifting": true,
            "edgeMinimization": true,
            "parentCentralization": true
            },
            "improvedLayout": true
        },
        "physics": {
            "enabled": true,
            "hierarchicalRepulsion": {
                "nodeDistance": 250,
                "springLength": 200,
                "centralGravity": 0.0,
                "springConstant": 0.01,
                "damping": 0.09
            },
            "minVelocity": 0.75,
            "solver": "hierarchicalRepulsion"
        },
        "nodes": {
            "scaling": {
            "min": 30,
            "max": 50
            },
            "margin": 10,
            "font": {
            "size": 14
            }
        },
        "edges": {
            "smooth": {
            "type": "cubicBezier",
            "roundness": 0.5
            }
        }
        }
        """
    )

    # Transfer the NetworkX graph to PyVis Network
    net.from_nx(G)

    # Customize nodes
    for node in net.nodes:
        node_id = node["id"]
        count = node_counts[node_id]
        node["label"] = f"{node_id}\n{count}"  # Display both name and count
        node["title"] = f"{node_id}: {count}"  # Show on hover
        node["value"] = count  # For sizing the node
        node["shape"] = "circle"  # Ensure the node shape is a circle
        node["color"] = get_node_color(node_id)
        node["font"] = {
            "size": 14,  # Reduced font size
            "color": "#000000",
            "face": "arial",
            "align": "center",
            "multi": "true",
            "bold": True,
        }

    # Customize edges
    for edge in net.edges:
        edge["color"] = {"color": "#A9A9A9"}
        edge["dash"] = True  # Set to True for dashed edges
        count = edge_counts[(edge["from"], edge["to"])]
        edge["label"] = str(count)  # Display count number
        edge["title"] = f"From {edge['from']} to {edge['to']}: {count} times"
        # Adjust label properties
        edge["font"] = {
            "size": 12,  # Reduced font size
            "align": "middle",
        }  # Position label in front of arrow
        edge["arrows"] = "to"  # Ensure arrows are shown
        edge["arrowStrikethrough"] = False
        edge["width"] = 1  # Set a base width for edges

    # Generate network graph HTML and add border to iframe
    try:
        html = net.generate_html()
        html = add_border_to_iframe(html)
    except Exception as e:
        st.error(f"Error generating network graph HTML: {e}")
        html = None

    # Display the network graph in Streamlit with increased width and height
    if html:
        components.html(html, height=810, width=1400)
    else:
        st.error("Failed to generate the network graph.")
else:
    st.info("Awaiting data...")
