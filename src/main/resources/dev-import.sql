-- Clear existing posts (optional - be careful with this!)
-- TRUNCATE TABLE tb_posts;

-- Post 1: First blog post - Published 3 months ago
INSERT INTO tb_posts(slug, title, author, content, published, created_at, updated_at, published_at) 
VALUES (
    'hello-world', 
    'Hello World! Welcome to My Blog', 
    'Victor Osório',
    '<h1>Welcome!</h1><p>This is my first blog post. I''m excited to start this journey of sharing knowledge about web development, programming, and technology.</p><h2>What to expect</h2><ul><li>Weekly tutorials on JavaScript and Python</li><li>Best practices in web development</li><li>Reviews of development tools</li><li>Personal projects and experiments</li></ul><p>Stay tuned for more content!</p>', 
    TRUE, 
    '2024-01-15 09:00:00', 
    '2024-01-15 09:00:00', 
    '2024-01-15 10:30:00'
);

-- Post 2: Technical tutorial - Published 2 months ago
INSERT INTO tb_posts(slug, title, author, content, published, created_at, updated_at, published_at) 
VALUES (
    'understanding-javascript-closures', 
    'Understanding JavaScript Closures', 
    'Victor Osório',
    '<h2>What are Closures?</h2><p>A closure is the combination of a function bundled together with references to its surrounding state. In JavaScript, closures are created every time a function is created.</p><h3>Simple Example</h3><pre><code>function outerFunction(x) { return function innerFunction(y) { return x + y; }; } const add5 = outerFunction(5); console.log(add5(3)); // 8</code></pre><h3>Practical Applications</h3><p>Closures are useful for:</p><ul><li>Data privacy and encapsulation</li><li>Function factories</li><li>Event handlers</li><li>Partial application</li></ul>', 
    TRUE, 
    '2024-02-01 14:30:00', 
    '2024-02-03 11:20:00', 
    '2024-02-05 08:15:00'
);

-- Post 3: Opinion piece - Published 1 month ago
INSERT INTO tb_posts(slug, title, author, content, published, created_at, updated_at, published_at) 
VALUES (
    'why-i-switched-to-vscode', 
    'Why I Switched to VS Code After 5 Years of Sublime Text', 
    'Victor Osório',
    '<p>After using Sublime Text for over 5 years, I finally made the switch to Visual Studio Code. Here''s why:</p><h3>Reasons for the Switch</h3><ul><li><strong>Built-in Git integration</strong> - No more switching to terminal for basic operations</li><li><strong>IntelliSense</strong> - Smart code completion that actually understands my codebase</li><li><strong>Extension ecosystem</strong> - Everything from Python debugging to Docker integration</li><li><strong>Regular updates</strong> - Microsoft has been consistently improving the editor</li></ul><p>Don''t get me wrong, Sublime Text is still an amazing editor, but VS Code has caught up and surpassed it in many areas that matter for my daily workflow.</p>', 
    TRUE, 
    '2024-02-20 16:45:00', 
    '2024-02-22 09:30:00', 
    '2024-02-25 11:00:00'
);

-- Post 4: Tutorial series - Published 2 weeks ago
INSERT INTO tb_posts(slug, title, author, content, published, created_at, updated_at, published_at) 
VALUES (
    'building-rest-api-python-flask-part-1', 
    'Building a REST API with Python and Flask - Part 1: Setup', 
    'Victor Osório',
    '<p>Welcome to this multi-part series on building a REST API with Flask!</p><h2>Part 1: Setting Up the Project</h2><h3>Prerequisites</h3><ul><li>Python 3.8+ installed</li><li>Basic understanding of Python</li><li>Virtual environment knowledge</li></ul><h3>Step 1: Create Project Directory</h3><pre><code>mkdir flask-api cd flask-api python -m venv venv source venv/bin/activate  # On Windows: venv\Scripts\activate</code></pre><h3>Step 2: Install Flask</h3><pre><code>pip install flask flask-restful</code></pre><h3>Step 3: Create Basic App</h3><pre><code>from flask import Flask app = Flask(__name__) @app.route(''/hello'') def hello(): return {"message": "Hello World"} if __name__ == ''__main__'': app.run(debug=True)</code></pre><p>In Part 2, we''ll add database integration and user authentication. Stay tuned!</p>', 
    TRUE, 
    '2024-03-01 10:00:00', 
    '2024-03-02 14:20:00', 
    '2024-03-05 09:45:00'
);

-- Post 5: Short announcement - Published 1 week ago
INSERT INTO tb_posts(slug, title, author, content, published, created_at, updated_at, published_at) 
VALUES (
    'new-course-announcement', 
    '🎓 New Course: Modern Web Development Bootcamp', 
    'Victor Osório',
    '<p>I''m thrilled to announce my new course: <strong>Modern Web Development Bootcamp 2024</strong>!</p><h3>What''s Covered?</h3><ul><li>React 18 with Hooks and Context API</li><li>Node.js and Express backend development</li><li>PostgreSQL and MongoDB databases</li><li>TypeScript for type-safe code</li><li>Docker for containerization</li><li>Deployment to AWS and Vercel</li></ul><p>Early bird pricing is available for the first 100 students. <a href="/courses/modern-web-dev">Learn more and enroll here</a>.</p><p>Use code <strong>EARLYBIRD30</strong> for 30% off!</p>', 
    TRUE, 
    '2024-03-10 08:15:00', 
    '2024-03-10 08:15:00', 
    '2024-03-11 12:00:00'
);

-- Post 6: Draft post - Not published yet
INSERT INTO tb_posts(slug, title, author, content, published, created_at, updated_at, published_at) 
VALUES (
    'docker-for-beginners', 
    'Docker for Beginners: A Complete Guide', 
    'Victor Osório',
    '<p>This is a work in progress. Coming soon: Everything you need to know to get started with Docker!</p><p>Topics to cover:</p><ul><li>What is containerization?</li><li>Docker vs Virtual Machines</li><li>Dockerfiles explained</li><li>Docker Compose for multi-container apps</li><li>Best practices and common pitfalls</li></ul>', 
    FALSE, 
    '2024-03-12 13:30:00', 
    '2024-03-14 10:15:00', 
    NULL
);

-- Post 7: Recent published post - Published yesterday
INSERT INTO tb_posts(slug, title, author, content, published, created_at, updated_at, published_at) 
VALUES (
    'tailwind-css-tips', 
    '5 Tailwind CSS Tips That Saved Me Hours of Development Time', 
    'Victor Osório',
    '<p>After using Tailwind CSS for over a year, I''ve discovered some patterns that significantly improve productivity. Here are my top 5 tips:</p><h3>1. Use @apply for Repeated Patterns</h3><pre><code>.btn-primary { @apply px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors; }</code></pre><h3>2. Create a Consistent Spacing Scale</h3><p>Define custom spacing values in tailwind.config.js for brand consistency.</p><h3>3. Leverage Group Hover for Complex Interactions</h3><pre><code>&lt;div class="group"&gt; &lt;div class="group-hover:opacity-100 opacity-0"&gt;Hover to reveal&lt;/div&gt; &lt;/div&gt;</code></pre><h3>4. Use JIT Mode for Faster Builds</h3><p>Just-In-Time mode generates styles on-demand and enables arbitrary values.</p><h3>5. Organize Classes with Plugins</h3><p>Use prettier-plugin-tailwindcss to automatically sort classes consistently.</p><p>What are your favorite Tailwind tips? Share them in the comments!</p>', 
    TRUE, 
    '2024-03-20 15:00:00', 
    '2024-03-20 17:30:00', 
    '2024-03-30 08:00:00'
);