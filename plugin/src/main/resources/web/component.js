document.addEventListener('DOMContentLoaded', () => {
  const urlParams = new URLSearchParams(window.location.search);
  const componentId = urlParams.get('id'); // Fetch the `id` from the query parameter.

  const componentTitle = document.getElementById('component-title');
  const componentInfo = document.createElement('div'); // Create an area for component info
  componentInfo.className = 'component-info';

  const wrapper = document.createElement('div');
  wrapper.className = 'categories-variants-wrapper';

  document.querySelector('main').appendChild(wrapper);
  document.querySelector('main').insertBefore(componentInfo, wrapper); // Place info below the title


  // Load the component details
  const loadComponentDetails = async () => {
    try {
      const response = await fetch('arkive-showcase.json');
      const data = await response.json();

      // Find the module and component
      const module = data.modules.find((mod) =>
        mod.items.some((item) => item.component.id === componentId)
      );

      if (!module) {
        componentTitle.textContent = 'Component not found';
        return;
      }

      const componentData = module.items.find((item) => item.component.id === componentId);
      displayComponentDetails(componentData, module.name);
    } catch (error) {
      console.error('Error loading component details:', error);
    }
  };

  // Display the component details
  const displayComponentDetails = (data, moduleName) => {
    const { component, snapshotPath, variants } = data;

    // Set the component title
    componentTitle.textContent = `Component: ${component.name}`;

    // Display component info
    componentInfo.innerHTML = `
      <p><strong>Package:</strong> ${component.packageName}</p>
      <p><strong>Group:</strong> ${component.group}</p>
      <p><strong>Tags:</strong> ${component.tags.join(', ') || 'None'}</p>
    `;

    // Group snapshots by category
    const categories = [
      { category: 'Base', variants: [{ variant: 'Default', snapshotPath }] },
      ...groupAndSortVariantsByCategory(variants),
    ];

    // Render categories and variants
    renderCategoriesAndVariants(categories, moduleName);
  };

  // Group and sort variants by category
  const groupAndSortVariantsByCategory = (variants) => {
    const grouped = {};

    // Group variants by category
    variants.forEach(({ category, variant, snapshotPath }) => {
      if (!grouped[category]) {
        grouped[category] = [];
      }
      grouped[category].push({ variant, snapshotPath });
    });

    // Sort categories and their variants
    return Object.keys(grouped).map((category) => ({
      category,
      variants: sortVariants(category, grouped[category]),
    }));
  };

  // Sort variants based on category (density, font, etc.)
  const sortVariants = (category, variants) => {
    const densityOrder = ['ldpi', 'mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi'];

    if (category.toLowerCase() === 'density') {
      return variants.sort(
        (a, b) => densityOrder.indexOf(a.variant.toLowerCase()) - densityOrder.indexOf(b.variant.toLowerCase())
      );
    }

    if (category.toLowerCase() === 'font') {
      return variants.sort(
        (a, b) => parseFloat(a.variant) - parseFloat(b.variant) // Numeric sort for font sizes
      );
    }

    // Default alphabetical sort for other categories
    return variants.sort((a, b) => a.variant.localeCompare(b.variant));
  };

  // Render categories and variants
  const renderCategoriesAndVariants = (categories, moduleName) => {
    categories.forEach(({ category, variants }) => {
      const row = document.createElement('div');
      row.className = 'category-row';

      const categoryName = document.createElement('div');
      categoryName.className = 'category-name';
      categoryName.textContent = `${category}:`;

      const variantsWrapper = document.createElement('div');
      variantsWrapper.className = 'variants';

      variants.forEach(({ variant, snapshotPath }) => {
        const variantDiv = document.createElement('div');
        variantDiv.className = 'variant';

        const imagePath = `${moduleName}/images/${snapshotPath.split('/').pop()}`;
        variantDiv.innerHTML = `
          <img src="${imagePath}" alt="${variant}" />
          <p>${variant}</p>
        `;

        variantsWrapper.appendChild(variantDiv);
      });

      row.appendChild(categoryName);
      row.appendChild(variantsWrapper);

      wrapper.appendChild(row);
    });
  };

  // Load the component on page load
  loadComponentDetails();
});