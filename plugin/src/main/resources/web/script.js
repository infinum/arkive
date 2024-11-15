document.addEventListener('DOMContentLoaded', () => {
  const grid = document.getElementById('grid');
  const searchBar = document.getElementById('search-bar');
  const filterDropdown = document.getElementById('filter-dropdown');

  let components = [];

  // Load JSON data
  const loadComponents = async () => {
    try {
      const response = await fetch('arkive-showcase.json');
      const data = await response.json();
      components = data.items || [];
      populateFilterOptions();
      displayComponents(components);
    } catch (error) {
      console.error('Error loading JSON:', error);
    }
  };

  // Populate unified filter dropdown
  const populateFilterOptions = () => {
    const options = new Set();

    components.forEach(({ component }) => {
      options.add(component.packageName);
      options.add(component.group);
      component.tags.forEach((tag) => options.add(tag));
    });

    options.forEach((option) => {
      const dropdownOption = document.createElement('option');
      dropdownOption.value = option;
      dropdownOption.textContent = option;
      filterDropdown.appendChild(dropdownOption);
    });
  };

  // Display components in the grid
  const displayComponents = (items) => {
    grid.innerHTML = '';
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

  // Filter components based on search and dropdown
  const filterComponents = () => {
    const query = searchBar.value.toLowerCase();
    const selectedFilter = filterDropdown.value.toLowerCase();

    const filtered = components.filter(({ component }) => {
      const { name, group, tags, packageName } = component;

      const matchesSearch =
        name.toLowerCase().includes(query) ||
        group.toLowerCase().includes(query) ||
        tags.some((tag) => tag.toLowerCase().includes(query)) ||
        packageName.toLowerCase().includes(query);

      const matchesFilter =
        !selectedFilter ||
        packageName.toLowerCase() === selectedFilter ||
        group.toLowerCase() === selectedFilter ||
        tags.some((tag) => tag.toLowerCase() === selectedFilter);

      return matchesSearch && matchesFilter;
    });

    displayComponents(filtered);
  };

  // Attach event listeners
  searchBar.addEventListener('input', filterComponents);
  filterDropdown.addEventListener('change', filterComponents);

  // Load components on page load
  loadComponents();
});