document.addEventListener('DOMContentLoaded', () => {
  const grid = document.getElementById('grid');
  const searchBar = document.getElementById('search-bar');

  let components = [];

  // Function to load JSON data
  const loadComponents = async () => {
    try {
      const response = await fetch('arkive-showcase.json'); // Updated file name
      const data = await response.json();
      components = data.items || [];
      displayComponents(components);
    } catch (error) {
      console.error('Error loading JSON:', error);
    }
  };

  // Function to display components
  const displayComponents = (items) => {
    grid.innerHTML = ''; // Clear existing grid content
    items.forEach(({ component, snapshotPath }) => {
      const gridItem = document.createElement('div');
      gridItem.className = 'grid-item';
      gridItem.innerHTML = `
        <img src="${snapshotPath}" alt="${component.name}" />
        <div class="name">${component.name}</div>
      `;
      grid.appendChild(gridItem);
    });
  };

  // Function to filter components based on search query
  const filterComponents = (query) => {
    const filtered = components.filter(({ component }) => {
      const { name, group, tags, extraMetadata, packageName } = component;
      const searchText = query.toLowerCase();
      return (
        name.toLowerCase().includes(searchText) ||
        group.toLowerCase().includes(searchText) ||
        tags.some((tag) => tag.toLowerCase().includes(searchText)) ||
        extraMetadata.some((meta) => meta.toLowerCase().includes(searchText)) ||
        packageName.toLowerCase().includes(searchText)
      );
    });
    displayComponents(filtered);
  };

  // Attach event listener to search bar
  searchBar.addEventListener('input', (e) => {
    const query = e.target.value;
    filterComponents(query);
  });

  // Load components on page load
  loadComponents();
});