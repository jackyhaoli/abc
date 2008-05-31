package org.jastadd.plugin.explorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.ImageImageDescriptor;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.eclipse.ui.views.navigator.ResourceNavigator;
import org.jastadd.plugin.AST.IJastAddNode;
import org.jastadd.plugin.AST.IOutlineNode;
import org.jastadd.plugin.editor.JastAddStorageEditorInput;
import org.jastadd.plugin.model.JastAddModel;
import org.jastadd.plugin.model.JastAddModelProvider;
import org.jastadd.plugin.util.JastAddEditorUtil;

public abstract class JastAddBaseExplorer extends ResourceNavigator implements
		IShowInTarget {

	protected void initResourceComparator() {
		setComparator(new ResourceComparator(ResourceComparator.NAME) {
			protected int compareNames(IResource resource1, IResource resource2) {
				return getComparator().compare(
						resource1.getFullPath().toString(),
						resource2.getFullPath().toString());
			}
		});
	}

	protected abstract IContainer findSourceRoot(IResource resource);

	protected abstract class BaseContentProvider implements
			ITreeContentProvider, IResourceChangeListener {
		protected final ITreeContentProvider resourceContentProvider;
		protected final ITreeContentProvider jastAddContentProvider;

		protected Viewer viewer;

		public BaseContentProvider(
				ITreeContentProvider resourceContentProvider,
				ITreeContentProvider jastAddContentProvider) {
			super();
			this.resourceContentProvider = resourceContentProvider;
			this.jastAddContentProvider = jastAddContentProvider;
		}

		protected void doViewerRefresh() {
			if (viewer == null)
				return;
			Control control = viewer.getControl();

			if (control.getDisplay().getThread() == Thread.currentThread()) {
				viewer.refresh();
			} else {
				control.getDisplay().asyncExec(new Runnable() {
					public void run() {
						Control ctrl = viewer.getControl();
						if (ctrl == null || ctrl.isDisposed())
							return;
						viewer.refresh();
					}
				});
			}
		}

		public synchronized void inputChanged(Viewer viewer, Object oldInput,
				Object newInput) {
			this.viewer = viewer;
			IWorkspace oldWorkspace = null;
			IWorkspace newWorkspace = null;

			if (oldInput instanceof IWorkspace) {
				oldWorkspace = (IWorkspace) oldInput;
			} else if (oldInput instanceof IContainer) {
				oldWorkspace = ((IContainer) oldInput).getWorkspace();
			}

			if (newInput instanceof IWorkspace) {
				newWorkspace = (IWorkspace) newInput;
			} else if (newInput instanceof IContainer) {
				newWorkspace = ((IContainer) newInput).getWorkspace();
			}

			if (oldWorkspace != newWorkspace) {
				if (oldWorkspace != null) {
					oldWorkspace.removeResourceChangeListener(this);
				}
				if (newWorkspace != null) {
					newWorkspace.addResourceChangeListener(this,
							IResourceChangeEvent.POST_CHANGE);
				}
			}
		}

		public synchronized void dispose() {
			if (viewer != null) {
				IWorkspace workspace = null;
				Object obj = viewer.getInput();
				if (obj instanceof IWorkspace) {
					workspace = (IWorkspace) obj;
				} else if (obj instanceof IContainer) {
					workspace = ((IContainer) obj).getWorkspace();
				}
				if (workspace != null) {
					workspace.removeResourceChangeListener(this);
				}
			}
		}

		public synchronized void resourceChanged(
				final IResourceChangeEvent event) {
			doViewerRefresh();
		}

		public Object[] getElements(Object element) {
			return getChildren(element);
		}

		public synchronized Object[] getChildren(Object element) {
			if (element instanceof IJastAddNode)
				return jastAddContentProvider.getChildren(element);

			IResource resource = (IResource) element;
			if(resource != null) {
			IContainer sourceRoot = findSourceRoot(resource);
			if (sourceRoot != null) {
				if (resource instanceof IContainer) {
					return getSourceRootItemChildren(resource, sourceRoot);
				} else if (resource instanceof IFile) {
					IFile file = (IFile) resource;
					JastAddModel model = JastAddModelProvider.getModel(file);
					if (model != null) {
						IJastAddNode node = model.getTreeRoot(model.buildFileInfo(file));
						return jastAddContentProvider.getChildren(node);
					}
				}
			}
			}

			return resourceContentProvider.getChildren(element);
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public synchronized Object getParent(Object element) {
			if (element instanceof IJastAddNode) {
				IJastAddNode node = (IJastAddNode) element;
				IJastAddNode parent = (IJastAddNode) jastAddContentProvider
						.getParent(element);
				if (parent != null)
					return parent;
				else {
					JastAddModel model = JastAddModelProvider.getModel(node);
					return model.getFile(node);
				}
			}

			IResource resource = (IResource) element;
			IContainer sourceRoot = findSourceRoot(resource);
			if (sourceRoot != null && !(sourceRoot.equals(resource)))
				return getSourceRootItemParent(sourceRoot, resource);

			return resourceContentProvider.getParent(element);
		}

		private Object[] getSourceRootItemChildren(IResource resource,
				IContainer sourceRoot) {
			IContainer container = (IContainer) resource;
			List<IResource> result = new ArrayList<IResource>();
			if (sourceRoot.equals(container)) {
				collectAllSubContainers(sourceRoot, result);
				collectDirectNonContainers(sourceRoot, result);
			} else {
				collectDirectNonContainers(container, result);
			}
			return result.toArray();
		}

		protected IContainer getSourceRootItemParent(IContainer sourceRoot,
				IResource resource) {
			if (resource instanceof IContainer)
				return sourceRoot;
			else
				return resource.getParent();
		}

		protected void collectDirectNonContainers(IContainer sourceRoot,
				List<IResource> result) {
			Object[] children = resourceContentProvider.getChildren(sourceRoot);
			for (Object child : children) {
				IResource childResource = (IResource) child;
				if (!(childResource instanceof IContainer))
					result.add(childResource);
			}
		}

		protected void collectAllSubContainers(IContainer container,
				List<IResource> result) {
			Object[] children = resourceContentProvider.getChildren(container);
			for (Object child : children) {
				IResource childResource = (IResource) child;
				if (childResource instanceof IContainer) {
					result.add(childResource);
					collectAllSubContainers((IContainer) childResource, result);
				}
			}
		}
	}

	protected class BaseLabelProvider extends LabelProvider {
		protected final ILabelProvider resourceLabelProvider;
		protected final ILabelProvider jastAddLabelProvider;

		public BaseLabelProvider(ILabelProvider resourceLabelProvider,
				ILabelProvider jastAddLabelProvider) {
			super();
			this.resourceLabelProvider = resourceLabelProvider;
			this.jastAddLabelProvider = jastAddLabelProvider;
		}

		public String getText(Object element) {
			if (element == null)
				return null;

			if (element instanceof IOutlineNode)
				return jastAddLabelProvider.getText(element);

			IResource resource = (IResource) element;

			IContainer sourceRoot = findSourceRoot(resource);
			if (sourceRoot != null)
				return getSourceRootItemText(sourceRoot, resource);

			return resourceLabelProvider.getText(resource);
		}

		protected String getSourceRootItemText(IContainer sourceRoot,
				IResource resource) {
			if (resource instanceof IContainer && !sourceRoot.equals(resource)) {
				IPath path = resource.getFullPath();
				path = path.removeFirstSegments(path
						.matchingFirstSegments(sourceRoot.getFullPath()));
				String[] segments = path.segments();
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < segments.length; i++) {
					if (i > 0)
						buffer.append(".");
					buffer.append(segments[i]);
				}
				return buffer.toString();
			} else
				return resourceLabelProvider.getText(resource);
		}

		public Image getImage(Object element) {
			if (element == null)
				return null;

			if (element instanceof IJastAddNode) {
				IJastAddNode node = (IJastAddNode) element;
				Image image = jastAddLabelProvider.getImage(node);
				return image;
			}

			IResource resource = (IResource) element;

			IContainer sourceRoot = findSourceRoot(resource);
			if (sourceRoot != null)
				return getSourceRootItemImage(sourceRoot, resource);

			return resourceLabelProvider.getImage(resource);
		}

		protected Image getSourceRootItemImage(IContainer sourceRoot,
				IResource resource) {
			return resourceLabelProvider.getImage(resource);
		}

	}

	protected class BaseProblemLabelDecorator extends LabelProvider implements
			ILabelDecorator {

		public String decorateText(String text, Object element) {
			return text;
		}

		public Image decorateImage(Image image, Object element) {
			if (image == null) return null;
			
			if (element instanceof IJastAddNode)
				return decorateJastAddNodeImage(image, (IJastAddNode) element);

			IResource resource = (IResource) element;

			IContainer sourceRoot = findSourceRoot(resource);
			if (sourceRoot != null)
				return decorateSourceRootItemImage(image, sourceRoot, resource);

			return decorateResourceImage(image, resource);

		}

		protected Image decorateResourceImage(Image image, IResource resource) {
			return decorateResourceImage(image, resource,
					IResource.DEPTH_INFINITE);
		}

		protected Image decorateJastAddNodeImage(Image image, IJastAddNode node) {
			JastAddModel model = JastAddModelProvider.getModel(node);
			if (model == null)
				return image;

			IFile file = model.getFile(node);
			if (file == null)
				return image;

			int severity = IMarker.SEVERITY_INFO;
			try {
				IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true,
						IResource.DEPTH_ZERO);

				for (IMarker marker : markers) {
					if (marker.getAttribute(IMarker.SEVERITY) == null) continue;
					int markerSeverity = ((Integer) marker
							.getAttribute(IMarker.SEVERITY)).intValue();
					
					if (marker.getAttribute(IMarker.CHAR_START) != null && marker.getAttribute(IMarker.CHAR_END) != null) {
						int markerStart = ((Integer) marker
								.getAttribute(IMarker.CHAR_START)).intValue();
						int markerEnd = ((Integer) marker
								.getAttribute(IMarker.CHAR_END)).intValue();					
						
						if (rangeIntersect(node.getBeginOffset(), node.getEndOffset(), markerStart, markerEnd))
							if (markerSeverity > severity)
								severity = markerSeverity;				
					}
					else if (marker.getAttribute(IMarker.LINE_NUMBER) != null) {
						int markerLine = ((Integer) marker
								.getAttribute(IMarker.LINE_NUMBER)).intValue();
						
						if (node.getBeginLine() <= markerLine && markerLine <= node.getEndLine())
							if (markerSeverity > severity)
								severity = markerSeverity;
						
					}
				}
			} catch (CoreException e) {
				return image;
			}

			return decorateImage(image, severity);
		}
		
		private boolean rangeIntersect(int start1, int end1, int start2, int end2) {
			if (start1 > end2 || end1 < start2)
				return false;
			return true;
		}

		protected Image decorateSourceRootItemImage(Image image,
				IContainer sourceRoot, IResource resource) {
			if (resource instanceof IContainer && !sourceRoot.equals(resource)) {
				return decorateResourceImage(image, resource,
						IResource.DEPTH_ONE);
			} else
				return decorateResourceImage(image, resource,
						IResource.DEPTH_INFINITE);
		}

		protected Image decorateResourceImage(Image image, IResource resource,
				int depth) {
			int severity;
			try {
				severity = resource.findMaxProblemSeverity(IMarker.PROBLEM,
						true, depth);
			} catch (CoreException e) {
				return image;
			}

			return decorateImage(image, severity);
		}

		protected Image decorateImage(Image image, int severity) {
			int adornmentFlags;
			if (severity == IMarker.SEVERITY_ERROR)
				adornmentFlags = JavaElementImageDescriptor.ERROR;
			else if (severity == IMarker.SEVERITY_WARNING)
				adornmentFlags = JavaElementImageDescriptor.WARNING;
			else
				return image;

			ImageDescriptor imageDescriptor = new ImageImageDescriptor(image);
			Rectangle bounds = image.getBounds();
			return JavaPlugin.getImageDescriptorRegistry().get(
					new JavaElementImageDescriptor(imageDescriptor,
							adornmentFlags, new Point(bounds.width,
									bounds.height)));
		}
	}

	protected void handleOpen(OpenEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		Object element = selection.getFirstElement();

		if (element instanceof IJastAddNode) {
			IJastAddNode node = (IJastAddNode) element;
			JastAddModel model = JastAddModelProvider.getModel(node);
			if (model != null)
				model.openFile(node);
		} else if (element instanceof IFile) {
			IFile file = (IFile) element;
			openFile(file);
		} else
			super.handleOpen(event);
	}

	private void openFile(IFile file) {
		TreeViewer viewer = getTreeViewer();
		IEditorDescriptor descriptor = JastAddEditorUtil
				.getEditorDescription(file);
		OpenFileAction action = new OpenFileAction(getSite().getPage(),
				descriptor);
		action.selectionChanged((IStructuredSelection) viewer
				.getSelection());
		if (action.isEnabled())
			action.run();
	}

	public boolean show(ShowInContext context) {
		ArrayList toSelect = new ArrayList();
		ISelection sel = context.getSelection();
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			for (Iterator i = ssel.iterator(); i.hasNext();) {
				Object o = i.next();
				if (o instanceof IResource) {
					toSelect.add(o);
				} else if (o instanceof IMarker) {
					IResource r = ((IMarker) o).getResource();
					if (r.getType() != IResource.ROOT) {
						toSelect.add(r);
					}
				} else if (o instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) o;
					o = adaptable.getAdapter(IResource.class);
					if (o instanceof IResource) {
						toSelect.add(o);
					} else {
						o = adaptable.getAdapter(IMarker.class);
						if (o instanceof IMarker) {
							IResource r = ((IMarker) o).getResource();
							if (r.getType() != IResource.ROOT) {
								toSelect.add(r);
							}
						}
					}
				}
			}
		} else if (sel instanceof ITextSelection) {
			ITextSelection tsel = (ITextSelection) sel;
			
			IFile file = null;
			if (context.getInput() instanceof FileEditorInput) {
				FileEditorInput editorInput = (FileEditorInput) context.getInput();
				file = editorInput.getFile();
			}
			else if (context.getInput() instanceof JastAddStorageEditorInput) {
				JastAddStorageEditorInput editorInput = (JastAddStorageEditorInput) context.getInput();
				IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(editorInput.getStorage().getFullPath());
				if (files.length == 1)
					file = files[0];
			}
			
			if (file != null) {
				IJastAddNode parent = null;
				JastAddModel model = JastAddModelProvider.getModel(file);
				if (model != null) {
					IJastAddNode node = model.findNodeInDocument(model
							.buildFileInfo(file), tsel.getOffset());
					if (node != null) {
						parent = node;
						while (parent != null
								&& !(parent instanceof IOutlineNode && ((IOutlineNode) parent)
										.showInContentOutline()))
							parent = parent.getParent();
					}
				}
				if (parent != null)
					toSelect.add(parent);
				else
					toSelect.add(file);
			}
		}

		if (toSelect.isEmpty()) {
			Object input = context.getInput();
			if (input instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) input;
				Object o = adaptable.getAdapter(IResource.class);
				if (o instanceof IResource) {
					toSelect.add(o);
				}
			}
		}

		if (!toSelect.isEmpty()) {
			selectReveal(new StructuredSelection(toSelect));
			return true;
		}
		return false;
	}

	public void selectReveal(ISelection selection) {
		StructuredSelection ssel = convertSelection(selection);
		if (!ssel.isEmpty()) {
			getViewer().getControl().setRedraw(false);
			getViewer().setSelection(ssel, true);
			getViewer().getControl().setRedraw(true);
		}
	}

	private StructuredSelection convertSelection(ISelection selection) {
		ArrayList list = new ArrayList();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			for (Iterator i = ssel.iterator(); i.hasNext();) {
				Object o = i.next();
				if (o instanceof IResource) {
					list.add(o);
				} else if (o instanceof IJastAddNode) {
					list.add(o);
				}
				if (o instanceof IAdaptable) {
					list.add(((IAdaptable) o).getAdapter(IResource.class));
				}
			}
		}
		return new StructuredSelection(list);
	}
}
