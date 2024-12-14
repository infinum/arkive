document.addEventListener('DOMContentLoaded', () => {
  const urlParams = new URLSearchParams(window.location.search);
  const moduleName = urlParams.get('module'); // Fetch the module name from the query string

  const moduleTitle = document.getElementById('module-title');
  const grid = document.getElementById('grid');
  const searchBar = document.querySelector('.search-bar');
  const filterDropdown = document.querySelector('.filter-dropdown');

  let allComponents = []; // Store all components for filtering

  // Load the module details
  const loadModuleDetails = async () => {
    try {
      const response = await fetch('arkive-showcase.json');
      const data = await response.json();

      // Find the module by name
      const module = data.modules.find((mod) => mod.name === moduleName);

      if (!module) {
        moduleTitle.textContent = 'Module not found';
        return;
      }

      displayModuleDetails(module);
      allComponents = module.items; // Save components for filtering
    } catch (error) {
      console.error('Error loading module details:', error);
    }
  };

  // Display the module details
  const displayModuleDetails = (module) => {
    moduleTitle.textContent = `Components in ${module.name}`;
    displayComponents(module.items);
  };

  // Display components in the grid
  const displayComponents = (components) => {
    grid.innerHTML = ''; // Clear existing items
    components.forEach(({ component, snapshotPath }) => {
      const componentCard = document.createElement('div');
      componentCard.className = 'grid-item';

      // Construct the image path based on the module's name
      const imagePath = `${moduleName}/images/${snapshotPath.split('/').pop()}`;

      componentCard.innerHTML = `
        <img src="${imagePath}" alt="${component.name}" />
        <div class="name">${component.name}</div>
      `;

      // Navigate to the component page when clicked
      componentCard.addEventListener('click', () => {
        window.location.href = `component.html?id=${component.id}`;
      });

      grid.appendChild(componentCard);
    });
  };

  // Filter components based on search and dropdown selection
  const filterComponents = () => {
    const searchValue = searchBar.value.toLowerCase();
    const filterValue = filterDropdown.value;

    let filteredComponents = allComponents;

    // Filter by search
    if (searchValue) {
      filteredComponents = filteredComponents.filter(({ component }) =>
        component.name.toLowerCase().includes(searchValue) ||
        component.group.toLowerCase().includes(searchValue) ||
        component.packageName.toLowerCase().includes(searchValue) ||
        component.tags.some((tag) => tag.toLowerCase().includes(searchValue))
      );
    }

    // Filter by dropdown selection
    if (filterValue !== 'all') {
      filteredComponents = filteredComponents.filter(({ component }) =>
        component.group.toLowerCase() === filterValue.toLowerCase() ||
        component.packageName.toLowerCase() === filterValue.toLowerCase() ||
        component.tags.includes(filterValue)
      );
    }

    displayComponents(filteredComponents);
  };

  // Event listeners for search and dropdown
  searchBar.addEventListener('input', filterComponents);
  filterDropdown.addEventListener('change', filterComponents);

  // Populate the filter dropdown with unique values
  const populateFilters = () => {
    const groups = new Set(allComponents.map(({ component }) => component.group));
    const packages = new Set(allComponents.map(({ component }) => component.packageName));
    const tags = new Set(allComponents.flatMap(({ component }) => component.tags));

    const uniqueValues = ['all', ...groups, ...packages, ...tags];

    filterDropdown.innerHTML = uniqueValues
      .map((value) => `<option value="${value}">${value}</option>`)
      .join('');
  };

  // Load the module on page load and populate filters
  loadModuleDetails().then(populateFilters);
});